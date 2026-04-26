package com.example.nlp;

import java.util.*;
import java.util.regex.*;

/**
 * NLP processor — used in TWO modes:
 *
 *   1. SEARCH MODE  (process / explain)
 *      Called by SearchService when a user types a query.
 *      Output: PostgreSQL to_tsquery string  e.g. "java | oop | programming"
 *
 *   2. INDEX MODE   (enrichMetadata)
 *      Called by PDFToDatabase when indexing a SCANNED PDF.
 *      Output: flat space-separated word list for to_tsvector()
 *      e.g. "bca bachelor computer application first semester end term 2018 java programming"
 *
 * Both modes share the same stopword list, stemmer, and synonym map.
 */
public class NLPQueryProcessor {

    // ── Stopwords ─────────────────────────────────────────────────────────────
    // NOTE: "mid", "end", "term", "sem", "year" are NOT stopwords for indexing —
    // they are meaningful for scanned paper metadata. They are only removed in
    // search mode to avoid over-filtering user queries.
    private static final Set<String> SEARCH_STOPWORDS = new HashSet<>(Arrays.asList(
        "a","an","the","is","are","was","were","be","been","being",
        "have","has","had","do","does","did","will","would","could","should",
        "may","might","shall","can","need","dare","ought","used",
        "i","me","my","we","our","you","your","he","she","it","they","them",
        "this","that","these","those","what","which","who","whom","whose",
        "in","on","at","by","for","with","about","against","between","into",
        "through","during","before","after","above","below","to","from",
        "up","down","of","off","over","under","again","further","then",
        "once","and","but","or","nor","so","yet","both","either","neither",
        "not","no","very","just","also","get","give","show","find"
    ));

    // Stopwords only for index enrichment (keep academic terms)
    private static final Set<String> INDEX_STOPWORDS = new HashSet<>(Arrays.asList(
        "a","an","the","is","are","was","were","be","been","being",
        "have","has","had","do","does","did","will","would","could","should",
        "i","me","my","we","our","you","your","he","she","it","they","them",
        "in","on","at","by","for","with","to","from","of","and","but","or",
        "not","no","very","just","also"
    ));

    // ── Synonym map — academic domain ─────────────────────────────────────────
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();
    static {
        // ── Programming languages & CS subjects ──────────────────────────────
        put("java",        "programming", "oop", "object", "oriented", "jdk", "jvm");
        put("python",      "programming", "scripting", "py");
        put("c",           "programming", "language", "clang");
        put("cpp",         "c++", "programming", "cplusplus");
        put("sql",         "database", "query", "rdbms", "structured");
        put("dbms",        "database", "sql", "rdbms", "management");
        put("database",    "dbms", "sql", "rdbms", "storage", "data");
        put("os",          "operating", "system", "kernel", "process");
        put("cn",          "network", "networking", "computer", "tcp", "ip");
        put("network",     "cn", "networking", "tcp", "ip", "protocol");
        put("ds",          "data", "structure", "algorithm", "array", "tree");
        put("algo",        "algorithm", "data", "structure", "complexity");
        put("algorithm",   "algo", "complexity", "sorting", "searching");
        put("ai",          "artificial", "intelligence", "machine", "learning");
        put("ml",          "machine", "learning", "ai", "neural", "model");
        put("web",         "html", "css", "javascript", "internet", "http");
        put("oop",         "object", "class", "inheritance", "java", "encapsulation");
        put("compiler",    "lexer", "parser", "syntax", "grammar", "token");
        put("software",    "engineering", "sdlc", "design", "development");
        put("math",        "mathematics", "calculus", "algebra", "discrete");
        put("maths",       "mathematics", "calculus", "algebra", "discrete");
        put("discrete",    "mathematics", "logic", "graph", "set", "relation");
        put("stats",       "statistics", "probability", "distribution");
        put("statistics",  "stats", "probability", "mean", "variance");
        put("electronics", "circuit", "digital", "analog", "signal");
        put("digital",     "electronics", "logic", "gate", "binary");
        put("accounts",    "accounting", "commerce", "finance", "ledger");
        put("finance",     "accounts", "commerce", "banking", "economics");
        put("economics",   "finance", "commerce", "micro", "macro");
        put("management",  "business", "administration", "mba", "hrm");
        put("marketing",   "management", "business", "sales", "commerce");
        put("biology",     "biotech", "biotechnology", "science", "life");
        put("biotech",     "biotechnology", "biology", "science", "genetic");

        // ── Degree abbreviations ──────────────────────────────────────────────
        put("bca",         "bachelor", "computer", "application", "bca");
        put("mca",         "master", "computer", "application", "mca");
        put("bsc",         "bachelor", "science", "bsc");
        put("msc",         "master", "science", "msc");
        put("bcom",        "bachelor", "commerce", "bcom");
        put("mba",         "master", "business", "administration", "mba");
        put("btech",       "bachelor", "technology", "engineering", "btech");
        put("mtech",       "master", "technology", "engineering", "mtech");
        put("it",          "information", "technology", "it");
        put("cs",          "computer", "science", "cs");

        // ── Semester / exam type ──────────────────────────────────────────────
        put("1st",         "first", "semester", "one");
        put("2nd",         "second", "semester", "two");
        put("3rd",         "third", "semester", "three");
        put("4th",         "fourth", "semester", "four");
        put("5th",         "fifth", "semester", "five");
        put("6th",         "sixth", "semester", "six");
        put("first",       "1st", "semester", "one");
        put("second",      "2nd", "semester", "two");
        put("third",       "3rd", "semester", "three");
        put("fourth",      "4th", "semester", "four");
        put("mid",         "midterm", "internal", "mid", "term");
        put("midterm",     "mid", "internal", "term");
        put("end",         "endterm", "final", "annual", "term");
        put("endterm",     "end", "final", "annual");
        put("final",       "end", "endterm", "annual");
        put("internal",    "mid", "midterm", "term");

        // ── Year tokens (self-referential so they survive stopword removal) ───
        for (int y = 2015; y <= 2025; y++) {
            put(String.valueOf(y), String.valueOf(y));
        }
    }

    private static void put(String key, String... values) {
        SYNONYMS.put(key, Arrays.asList(values));
    }

    // ── Suffix stemmer (Porter-lite) ──────────────────────────────────────────
    public static String stem(String word) {
        if (word.length() <= 3) return word;
        if (word.endsWith("ing") && word.length() > 5)
            return word.substring(0, word.length() - 3);
        if (word.endsWith("tion") || word.endsWith("sion"))
            return word.substring(0, word.length() - 3);
        if (word.endsWith("ment") && word.length() > 6)
            return word.substring(0, word.length() - 4);
        if (word.endsWith("ness") && word.length() > 6)
            return word.substring(0, word.length() - 4);
        if (word.endsWith("ly") && word.length() > 4)
            return word.substring(0, word.length() - 2);
        if (word.endsWith("ed") && word.length() > 4)
            return word.substring(0, word.length() - 2);
        if (word.endsWith("es") && !word.endsWith("ies") && word.length() > 4)
            return word.substring(0, word.length() - 2);
        if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 4)
            return word.substring(0, word.length() - 1);
        return word;
    }

    // ── Intent detection ──────────────────────────────────────────────────────
    public enum Intent {
        SUBJECT_SEARCH,   // "java notes", "dbms paper"
        YEAR_FILTER,      // "2022 question paper"
        DEPT_FILTER,      // "bca 1st sem"
        GENERAL
    }

    public static Intent detectIntent(String raw) {
        String lower = raw.toLowerCase();
        if (lower.matches(".*\\b(20\\d{2})\\b.*"))
            return Intent.YEAR_FILTER;
        if (lower.matches(".*\\b(bca|mca|bsc|msc|bcom|btech|mtech|mba)\\b.*"))
            return Intent.DEPT_FILTER;
        if (lower.matches(".*\\b(note|notes|paper|papers|exam|exams|test|tests)\\b.*"))
            return Intent.SUBJECT_SEARCH;
        return Intent.GENERAL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 1 — SEARCH: produces a to_tsquery string
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Processes a user search query into a PostgreSQL to_tsquery string.
     * e.g. "bca 2018 end term" → "bca | bachelor | computer | application | 2018 | end | endterm | final"
     */
    public static String process(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return "";

        String[] tokens = rawQuery.toLowerCase()
                                  .replaceAll("[^a-z0-9\\s]", " ")
                                  .trim()
                                  .split("\\s+");

        Set<String> expanded = new LinkedHashSet<>();

        for (String token : tokens) {
            if (token.isBlank() || SEARCH_STOPWORDS.contains(token)) continue;

            String stemmed = stem(token);
            expanded.add(token);
            if (!stemmed.equals(token)) expanded.add(stemmed);

            if (SYNONYMS.containsKey(token))   expanded.addAll(SYNONYMS.get(token));
            if (SYNONYMS.containsKey(stemmed)) expanded.addAll(SYNONYMS.get(stemmed));
        }

        if (expanded.isEmpty()) return rawQuery.trim();

        StringBuilder sb = new StringBuilder();
        for (String term : expanded) {
            if (sb.length() > 0) sb.append(" | ");
            // Wrap multi-word phrases in single quotes for tsquery
            sb.append(term.contains(" ") ? "'" + term + "'" : term);
        }
        return sb.toString();
    }

    /** Human-readable explanation shown in the status bar. */
    public static String explain(String rawQuery) {
        String processed = process(rawQuery);
        Intent intent = detectIntent(rawQuery);
        return String.format("NLP [%s]: \"%s\" → %s", intent.name(), rawQuery, processed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE 2 — INDEX: enriches scanned PDF metadata for to_tsvector()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Takes raw metadata text (filename + folder path tokens) and returns
     * an NLP-enriched flat word list suitable for to_tsvector().
     *
     * Unlike process(), this:
     *   - Uses a lighter stopword list (keeps "mid", "end", "term", "sem")
     *   - Does NOT produce tsquery syntax (no | operators)
     *   - Adds ALL synonym expansions as plain words
     *   - Preserves year tokens
     *   - Deduplicates while preserving order
     *
     * Example:
     *   Input:  "1ST SEM END BCA 2018 BCA BSC IT MCA MSC IT 1ST SEMESTER END TERM"
     *   Output: "1st sem end bca 2018 bsc it mca msc semester term
     *            first one bachelor computer application master science
     *            information technology endterm final annual midterm internal"
     */
    public static String enrichMetadata(String rawMetadata) {
        if (rawMetadata == null || rawMetadata.isBlank()) return "";

        // Extract years first before lowercasing strips context
        Set<String> years = new LinkedHashSet<>();
        Matcher yearMatcher = Pattern.compile("\\b(20\\d{2})\\b").matcher(rawMetadata);
        while (yearMatcher.find()) years.add(yearMatcher.group());

        String[] tokens = rawMetadata.toLowerCase()
                                     .replaceAll("[^a-z0-9\\s]", " ")
                                     .trim()
                                     .split("\\s+");

        Set<String> enriched = new LinkedHashSet<>();

        // Always add years first — they are the most specific search terms
        enriched.addAll(years);

        for (String token : tokens) {
            if (token.isBlank() || INDEX_STOPWORDS.contains(token)) continue;

            // Add the token itself
            enriched.add(token);

            // Add its stem
            String stemmed = stem(token);
            if (!stemmed.equals(token)) enriched.add(stemmed);

            // Add all synonyms
            if (SYNONYMS.containsKey(token)) {
                enriched.addAll(SYNONYMS.get(token));
            }
            if (SYNONYMS.containsKey(stemmed)) {
                enriched.addAll(SYNONYMS.get(stemmed));
            }
        }

        return String.join(" ", enriched);
    }
}
