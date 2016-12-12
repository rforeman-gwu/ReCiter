package reciter.service;

import java.util.List;

import reciter.model.pubmed.PubMedArticle;

public interface PubMedService {

	void save(List<PubMedArticle> pubMedArticles);
	
	List<PubMedArticle> findByPmids(List<Long> pmids);
}
