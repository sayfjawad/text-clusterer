# text-clusterer

A pluggable Java library for grouping strings by structural, lexical, n-gram, and phonetic similarity. No runtime dependencies.

---

## Table of contents

- [Overview](#overview)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Clusterers](#clusterers)
  - [SubstringClusterer](#substringclusterer)
  - [SubstringClustererBruteForce](#substringclustererbruteforce)
  - [NgramClusterer](#ngramclusterer)
  - [SemanticClusterer](#semanticclusterer)
  - [PhoneticClusterer](#phoneticclusterer)
- [Reading the result](#reading-the-result)
- [Parameter reference](#parameter-reference)
- [Implementing a custom strategy](#implementing-a-custom-strategy)
- [Choosing a strategy](#choosing-a-strategy)
- [Usage notes](#usage-notes)

---

## Overview

All clusterers implement the same interface:

```java
public interface TextClusterer {
    ClusterResult cluster(List<String> input, int minimalDenominatorLength, int minimumGroupSize);
}
```

Every call returns a `ClusterResult` containing:
- **groups** — strings that could be grouped, keyed by the common denominator that defines each group
- **ungrouped** — strings that did not meet the grouping criteria

Grouping is iterative: after each round the grouped strings are removed and the remainder is re-examined, so one call can produce multiple groups from a single input list.

---

## Installation

> Once published to Maven Central, add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>nl.multicode</groupId>
    <artifactId>text-clusterer</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Requirements:** Java 21+, no transitive runtime dependencies.

---

## Quick start

```java
import nl.multicode.SubstringClusterer;
import nl.multicode.TextClusterer;
import nl.multicode.model.ClusterResult;

List<String> logs = List.of(
    "Order created successfully",
    "Order created with warnings",
    "Order created and dispatched",
    "Payment gateway timeout"
);

TextClusterer clusterer = new SubstringClusterer();
ClusterResult result = clusterer.cluster(logs, 5, 2);

result.getGroups().forEach((key, group) -> {
    System.out.println("Group [" + key + "]:");
    group.values().forEach(s -> System.out.println("  " + s));
});

System.out.println("Ungrouped:");
result.getUngrouped().values().forEach(s -> System.out.println("  " + s));
```

Output:
```
Group [Order created]:
  Order created successfully
  Order created with warnings
  Order created and dispatched
Ungrouped:
  Payment gateway timeout
```

---

## Clusterers

### SubstringClusterer

Groups strings by their **longest common exact substring** using a Generalized Suffix Automaton (SAM). This is the most powerful and efficient strategy — it finds the longest substring shared across the most strings without enumerating all substrings.

**Best for:** log line grouping, deduplicating templated messages, finding structural patterns in text.

```java
TextClusterer clusterer = new SubstringClusterer();

List<String> input = List.of(
    "Error 502: Gateway bad response",
    "Error 504: Gateway timeout",
    "User login successful"
);

// minimalDenominatorLength = 5  (ignore common substrings shorter than 5 chars)
// minimumGroupSize          = 2  (need at least 2 strings to form a group)
ClusterResult result = clusterer.cluster(input, 5, 2);

// Groups: {"Error": {0: "Error 502...", 1: "Error 504..."}}  (or longer key)
// Ungrouped: {2: "User login successful"}
```

The key in each group is the longest substring that satisfies both constraints. Because the algorithm trims whitespace from substrings, leading/trailing spaces in the denominator are ignored.

**Performance:** O(n · L) where n is the number of strings and L is their average length. Handles hundreds of strings of moderate length well within milliseconds.

---

### SubstringClustererBruteForce

Same grouping logic as `SubstringClusterer` but implemented by enumerating all substrings explicitly. Produces identical results.

**Best for:** unit testing, verification, or environments where you prefer a simple readable implementation over algorithmic efficiency.

```java
TextClusterer clusterer = new SubstringClustererBruteForce();

ClusterResult result = clusterer.cluster(input, 5, 2);
```

> **Note:** Avoid on large inputs. Time complexity is O(n · L²) due to full substring enumeration.

---

### NgramClusterer

Groups strings by **shared character n-grams** — fixed-length substrings of size `n`. The group key is the n-gram shared by the most strings in each round.

**Best for:** typo-tolerant grouping, partial-match clustering, short strings where exact substring matching is too strict.

```java
TextClusterer clusterer = new NgramClusterer();

List<String> input = List.of(
    "football match",
    "footprint analysis",
    "basketball game"
);

// minimalDenominatorLength = n = 4  (use 4-character grams)
// minimumGroupSize          = 2
ClusterResult result = clusterer.cluster(input, 4, 2);

// "foot" is a 4-gram shared by index 0 and 1
result.getGroups().forEach((gram, group) -> {
    System.out.println("Shared 4-gram: " + gram);
    group.values().forEach(System.out::println);
});
```

**Choosing n:**
| n | Name | Use case |
|---|------|----------|
| 2 | bigram | Very loose matching; noisy on short strings |
| 3 | trigram | Good general-purpose default |
| 4–5 | 4/5-gram | Tighter matching, fewer false positives |

---

### SemanticClusterer

Groups strings by **shared words** (lexical/token overlap). Tokenizes each string by splitting on non-alphanumeric characters, lowercases all tokens, and groups strings that share the word seen by the most strings in each round.

**Best for:** natural language sentences, log messages with domain keywords, any text where meaning is carried by words rather than raw character runs.

```java
TextClusterer clusterer = new SemanticClusterer();

List<String> input = List.of(
    "ERROR detected in module A",
    "ERROR logged by module B",
    "Connection timeout"
);

// minimalDenominatorLength = 4  (ignore tokens shorter than 4 chars, e.g. "in", "by")
// minimumGroupSize          = 2
ClusterResult result = clusterer.cluster(input, 4, 2);

// Groups: {"error": {0: "ERROR detected...", 1: "ERROR logged..."}}
// Ungrouped: {2: "Connection timeout"}
```

**`minimalDenominatorLength` as a stop-word filter:**

Setting a higher minimum word length effectively filters common short words:

```java
// Filters out "a", "an", "is", "the", "in", "by", "on", "of", etc.
clusterer.cluster(input, 4, 2);

// Only tokens of 6+ characters are considered
clusterer.cluster(input, 6, 2);
```

> **Note:** This is structural word-overlap clustering, not embedding-based semantic similarity. Synonyms ("error" vs "failure") will not be matched. For true semantic similarity, provide your own `TextClusterer` implementation backed by an embedding model.

---

### PhoneticClusterer

Groups strings by **phonetic similarity** using the [Soundex](https://en.wikipedia.org/wiki/Soundex) algorithm. Each word in a string is encoded to a 4-character Soundex code (e.g. `R163`). Strings whose words share a code are grouped together.

**Best for:** name deduplication, spelling variant grouping, matching data where the same word appears in multiple phonetic spellings.

```java
TextClusterer clusterer = new PhoneticClusterer();

List<String> names = List.of(
    "Robert Johnson",
    "Rupert Johnston",
    "Alice Wong"
);

// minimalDenominatorLength = 3  (only encode words of 3+ characters)
// minimumGroupSize          = 2
ClusterResult result = clusterer.cluster(names, 3, 2);

// "Robert" and "Rupert" both encode to R163
// "Johnson" and "Johnston" both encode to J525
// Result groups by whichever code covers the most strings
result.getGroups().forEach((code, group) -> {
    System.out.println("Soundex code: " + code);
    group.values().forEach(System.out::println);
});
```

**Known Soundex values for reference:**

| Word | Soundex |
|------|---------|
| Robert | R163 |
| Rupert | R163 |
| Smith | S530 |
| Smyth | S530 |
| Euler | E460 |
| Ellery | E460 |
| Johnson | J525 |
| Johnston | J523 |

> **Note:** Soundex is designed for English. Results on non-English names may be inconsistent.

---

## Reading the result

```java
ClusterResult result = clusterer.cluster(input, minLen, minGroupSize);

// Check if any groups were found
if (result.hasGroups()) {

    // Iterate over groups
    // Key   = the common denominator (substring / n-gram / word / soundex code)
    // Value = map of original index -> original string
    result.getGroups().forEach((key, group) -> {
        System.out.println("Key: " + key);
        group.forEach((index, text) ->
            System.out.println("  [" + index + "] " + text)
        );
    });
}

// Strings that did not belong to any group
result.getUngrouped().forEach((index, text) ->
    System.out.println("Ungrouped [" + index + "]: " + text)
);
```

The integer keys in `getGroups()` and `getUngrouped()` are the **original 0-based positions** from the input list, so you can correlate results back to your source data.

---

## Parameter reference

| Parameter | All strategies | Strategy-specific meaning |
|---|---|---|
| `minimalDenominatorLength` | Minimum "size" of the common element | Substring length / n-gram size / word length / min word length for Soundex |
| `minimumGroupSize` | Minimum number of strings required to form a group | Same across all strategies |

Setting `minimumGroupSize = 2` means any pair of strings with a common denominator can form a group. Raising it to `3` or higher requires broader consensus before grouping occurs.

---

## Implementing a custom strategy

Implement `TextClusterer` to plug in your own logic:

```java
public class EmbeddingClusterer implements TextClusterer {

    private final EmbeddingModel model; // your choice of model

    public EmbeddingClusterer(EmbeddingModel model) {
        this.model = model;
    }

    @Override
    public ClusterResult cluster(List<String> input, int minimalDenominatorLength, int minimumGroupSize) {
        // compute embeddings, cluster by cosine similarity, return ClusterResult
    }
}
```

`ClusterResult` takes two maps directly:

```java
Map<String, Map<Integer, String>> groups   = new LinkedHashMap<>();
Map<Integer, String>              ungrouped = new LinkedHashMap<>();

// populate groups and ungrouped ...

return new ClusterResult(groups, ungrouped);
```

---

## Choosing a strategy

| Scenario | Recommended strategy |
|---|---|
| Log line grouping / templated messages | `SubstringClusterer` |
| Short strings, typo tolerance | `NgramClusterer` (n=3) |
| Natural language, keyword-based grouping | `SemanticClusterer` |
| Name deduplication, spelling variants | `PhoneticClusterer` |
| Reference / debugging / small inputs | `SubstringClustererBruteForce` |
| Meaning-aware clustering | Custom `TextClusterer` + embedding model |

---

## Usage notes

- **Input order is preserved.** The integer keys in `ClusterResult` reflect the original 0-based index of each string in the input list.
- **Groups are produced iteratively.** After each group is formed, its members are removed from the pool and the remaining strings are re-examined. This means a single call can return multiple disjoint groups.
- **A string belongs to at most one group.** Once grouped, a string is not reconsidered in subsequent rounds.
- **`null` input is treated as empty.** Both `null` and empty lists return a `ClusterResult` with empty groups and empty ungrouped.
- **`minimalDenominatorLength` as noise control.** A low value (e.g. 2) allows very short common denominators and will produce more — but potentially less meaningful — groups. A higher value enforces that only substantial common elements drive grouping.
- **`SubstringClusterer` vs `NgramClusterer`.** Substring clustering finds the *longest* common run; n-gram clustering finds the *most shared* fixed-size fragment. Use substring clustering when you care about the longest structural match; use n-gram clustering when you want consistent fragment size across groups.
- **`SemanticClusterer` is not embedding-based.** It groups by shared words, which means it is sensitive to exact word form. "error" and "errors" will not match; "error" and "failure" will not match. Stemming or lemmatisation is left to the caller if needed.
- **`PhoneticClusterer` uses Soundex.** Soundex is a well-known but imperfect algorithm — it was designed for English names and can produce false matches on non-English text or technical terms. For higher accuracy consider implementing a custom `TextClusterer` backed by Metaphone or Double Metaphone.
