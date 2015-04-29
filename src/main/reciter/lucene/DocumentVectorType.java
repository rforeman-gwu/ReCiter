package main.reciter.lucene;

/**
 * Indexable field names.
 * @author jil3004
 *
 */
public enum DocumentVectorType {
	ARTICLE_TITLE,
	ARTICLE_TITLE_UNTOKENIZED,
	JOURNAL_TITLE,
	JOURNAL_TITLE_UNTOKENIZED,
	JOURNAL_ISSUE_PUBDATE_YEAR,
	KEYWORD,
	KEYWORD_UNTOKENIZED,
	AFFILIATION,
	AFFILIATION_UNTOKENIZED,
	PMID,
	AUTHOR,
	AUTHOR_SIZE,
}
