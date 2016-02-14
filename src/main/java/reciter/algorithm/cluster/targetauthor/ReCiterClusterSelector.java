package reciter.algorithm.cluster.targetauthor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.evidence.StrategyContext;
import reciter.algorithm.evidence.article.ReCiterArticleStrategyContext;
import reciter.algorithm.evidence.article.RemoveReCiterArticleStrategyContext;
import reciter.algorithm.evidence.article.coauthor.CoauthorStrategyContext;
import reciter.algorithm.evidence.article.coauthor.strategy.CoauthorStrategy;
import reciter.algorithm.evidence.article.journal.JournalStrategyContext;
import reciter.algorithm.evidence.article.journal.strategy.JournalStrategy;
import reciter.algorithm.evidence.cluster.RemoveClusterStrategyContext;
import reciter.algorithm.evidence.cluster.clustersize.ClusterSizeStrategyContext;
import reciter.algorithm.evidence.cluster.clustersize.strategy.ClusterSizeStrategy;
import reciter.algorithm.evidence.targetauthor.TargetAuthorStrategyContext;
import reciter.algorithm.evidence.targetauthor.affiliation.AffiliationStrategyContext;
import reciter.algorithm.evidence.targetauthor.affiliation.strategy.WeillCornellAffiliationStrategy;
import reciter.algorithm.evidence.targetauthor.articlesize.ArticleSizeStrategyContext;
import reciter.algorithm.evidence.targetauthor.articlesize.strategy.ArticleSizeStrategy;
import reciter.algorithm.evidence.targetauthor.citizenship.CitizenshipStrategyContext;
import reciter.algorithm.evidence.targetauthor.citizenship.strategy.CitizenshipStrategy;
import reciter.algorithm.evidence.targetauthor.degree.DegreeStrategyContext;
import reciter.algorithm.evidence.targetauthor.degree.strategy.DegreeType;
import reciter.algorithm.evidence.targetauthor.degree.strategy.YearDiscrepancyStrategy;
import reciter.algorithm.evidence.targetauthor.department.DepartmentStrategyContext;
import reciter.algorithm.evidence.targetauthor.department.strategy.DepartmentStringMatchStrategy;
import reciter.algorithm.evidence.targetauthor.education.EducationStrategyContext;
import reciter.algorithm.evidence.targetauthor.education.strategy.EducationStrategy;
import reciter.algorithm.evidence.targetauthor.email.EmailStrategyContext;
import reciter.algorithm.evidence.targetauthor.email.strategy.EmailStringMatchStrategy;
import reciter.algorithm.evidence.targetauthor.grant.GrantStrategyContext;
import reciter.algorithm.evidence.targetauthor.grant.strategy.KnownCoinvestigatorStrategy;
import reciter.algorithm.evidence.targetauthor.name.RemoveByNameStrategyContext;
import reciter.algorithm.evidence.targetauthor.name.strategy.RemoveByNameStrategy;
import reciter.algorithm.evidence.targetauthor.scopus.ScopusStrategyContext;
import reciter.algorithm.evidence.targetauthor.scopus.strategy.StringMatchingAffiliation;
import reciter.model.article.ReCiterArticle;
import reciter.model.author.ReCiterAuthor;
import reciter.model.author.TargetAuthor;

public class ReCiterClusterSelector extends AbstractClusterSelector {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(ReCiterClusterSelector.class);

	/** Cluster selection strategy contexts. */

	/**
	 * Email Strategy.
	 */
	private StrategyContext emailStrategyContext;

	/**
	 * Department Strategy.
	 */
	private StrategyContext departmentStringMatchStrategyContext;

	/**
	 * Known co-investigator strategy context.
	 */
	private StrategyContext grantCoauthorStrategyContext;

	/**
	 * Affiliation strategy context.
	 */
	private StrategyContext affiliationStrategyContext;

	/** Individual article selection strategy contexts. */
	/**
	 * Scopus strategy context.
	 */
	private StrategyContext scopusStrategyContext;

	/**
	 * Coauthor strategy context.
	 */
	private StrategyContext coauthorStrategyContext;

	/**
	 * Journal strategy context.
	 */
	private StrategyContext journalStrategyContext;

	/**
	 * Citizenship strategy context.
	 */
	private StrategyContext citizenshipStrategyContext;

	/**
	 * Year Discrepancy (Bachelors).
	 */
	private StrategyContext bachelorsYearDiscrepancyStrategyContext;

	/**
	 * Year Discrepancy (Doctoral).
	 */
	private StrategyContext doctoralYearDiscrepancyStrategyContext;

	/**
	 * Discounts Articles not in English.
	 */
	private StrategyContext articleTitleInEnglishStrategyContext;

	/**
	 * Education.
	 */
	private StrategyContext educationStrategyContext;

	/**
	 * Remove article if the full first name doesn't match.
	 */
	private StrategyContext removeByNameStrategyContext;

	/**
	 * Article size.
	 */
	private StrategyContext articleSizeStrategyContext;

	/**
	 * Remove clusters based on cluster information.
	 */
	private StrategyContext clusterSizeStrategyContext;

	//	private StrategyContext boardCertificationStrategyContext;
	//
	//	private StrategyContext degreeStrategyContext;
	//	
	private List<StrategyContext> strategyContexts;

	private Set<Integer> selectedClusterIds; // List of currently selected cluster ids.

	public ReCiterClusterSelector(Map<Integer, ReCiterCluster> clusters, TargetAuthor targetAuthor) {

		// Strategies that select clusters that are similar to the target author.
		emailStrategyContext = new EmailStrategyContext(new EmailStringMatchStrategy());
		departmentStringMatchStrategyContext = new DepartmentStrategyContext(new DepartmentStringMatchStrategy());
		grantCoauthorStrategyContext = new GrantStrategyContext(new KnownCoinvestigatorStrategy());
		affiliationStrategyContext = new AffiliationStrategyContext(new WeillCornellAffiliationStrategy());

		// Using the following strategy contexts in sequence to reassign individual articles
		// to selected clusters.
		scopusStrategyContext = new ScopusStrategyContext(new StringMatchingAffiliation());
		coauthorStrategyContext = new CoauthorStrategyContext(new CoauthorStrategy(targetAuthor));
		journalStrategyContext = new JournalStrategyContext(new JournalStrategy(targetAuthor));
		citizenshipStrategyContext = new CitizenshipStrategyContext(new CitizenshipStrategy());
		educationStrategyContext = new EducationStrategyContext(new EducationStrategy());
		articleSizeStrategyContext = new ArticleSizeStrategyContext(new ArticleSizeStrategy());

		// TODO: getBoardCertificationScore(map);

		// bachelorsYearDiscrepancyStrategyContext = new DegreeStrategyContext(new YearDiscrepancyStrategy(DegreeType.BACHELORS));
		doctoralYearDiscrepancyStrategyContext = new DegreeStrategyContext(new YearDiscrepancyStrategy(DegreeType.DOCTORAL));
		// articleTitleInEnglishStrategyContext = new ArticleTitleStrategyContext(new ArticleTitleInEnglish());
		removeByNameStrategyContext = new RemoveByNameStrategyContext(new RemoveByNameStrategy());

		clusterSizeStrategyContext = new ClusterSizeStrategyContext(new ClusterSizeStrategy());

		strategyContexts = new ArrayList<StrategyContext>();
		strategyContexts.add(scopusStrategyContext);
		strategyContexts.add(coauthorStrategyContext);
		strategyContexts.add(journalStrategyContext);
		strategyContexts.add(citizenshipStrategyContext);
		//		strategyContexts.add(educationStrategyContext);
		strategyContexts.add(articleSizeStrategyContext);

		strategyContexts.add(bachelorsYearDiscrepancyStrategyContext);
		strategyContexts.add(doctoralYearDiscrepancyStrategyContext);
		//		strategyContexts.add(articleTitleInEnglishStrategyContext);
		strategyContexts.add(removeByNameStrategyContext);

		// Re-run these evidence types (could have been removed or not processed in sequence).
		strategyContexts.add(emailStrategyContext);
		//		strategyContexts.add(affiliationStrategyContext);

		// https://github.com/wcmc-its/ReCiter/issues/136
		strategyContexts.add(clusterSizeStrategyContext);
	}

	public void runStrategy(StrategyContext strategyContext, List<ReCiterArticle> reCiterArticles, TargetAuthor targetAuthor) {
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			if (strategyContext instanceof TargetAuthorStrategyContext) {
				((TargetAuthorStrategyContext) strategyContext).executeStrategy(reCiterArticle, targetAuthor);
			}
		}
	}

	@Override
	public void runSelectionStrategy(Map<Integer, ReCiterCluster> clusters, TargetAuthor targetAuthor) {
		// Select clusters that are similar to the target author.
		selectClusters(clusters, targetAuthor);

		// If no cluster ids are selected, select the cluster with the first name and middle name matches.
		selectClustersFallBack(clusters, targetAuthor);

		// Reassign individual article that are similar to the target author. 
		reAssignArticles(strategyContexts, clusters, targetAuthor);
	}

	/**
	 * Selecting clusters based on evidence types.
	 * 
	 * @param clusters Clusters formed in Phase I clustering.
	 * @param targetAuthor Target author.
	 * 
	 * @return A set of cluster ids that are selected because of the cluster's
	 * similarity to target author.
	 */
	public void selectClusters(Map<Integer, ReCiterCluster> clusters, TargetAuthor targetAuthor) {

		Set<Integer> selectedClusterIds = new HashSet<Integer>();
		for (Entry<Integer, ReCiterCluster> entry : clusters.entrySet()) {
			int clusterId = entry.getKey();
			List<ReCiterArticle> reCiterArticles = entry.getValue().getArticleCluster();

			double emailStrategyScore = ((TargetAuthorStrategyContext) emailStrategyContext).executeStrategy(reCiterArticles, targetAuthor);
			if (emailStrategyScore > 0) {
				selectedClusterIds.add(clusterId);
			}

			double departmentStrategyScore = ((TargetAuthorStrategyContext) departmentStringMatchStrategyContext).executeStrategy(reCiterArticles, targetAuthor);
			if (departmentStrategyScore > 0) {
				selectedClusterIds.add(clusterId);
			}

			double knownCoinvestigatorStrategyScore = ((TargetAuthorStrategyContext) grantCoauthorStrategyContext).executeStrategy(reCiterArticles, targetAuthor);
			if (knownCoinvestigatorStrategyScore > 0) {
				selectedClusterIds.add(clusterId);
			}

			double affiliationScore = ((TargetAuthorStrategyContext)affiliationStrategyContext).executeStrategy(reCiterArticles, targetAuthor);
			if (affiliationScore > 0) {
				selectedClusterIds.add(clusterId);
			}
		}
		this.selectedClusterIds = selectedClusterIds;
	}

	public void selectClustersFallBack(Map<Integer, ReCiterCluster> clusters, TargetAuthor targetAuthor) {
		if (selectedClusterIds.size() == 0) {
			for (Entry<Integer, ReCiterCluster> entry : clusters.entrySet()) {
				for (ReCiterArticle reCiterArticle : entry.getValue().getArticleCluster()) {
					for (ReCiterAuthor reCiterAuthor : reCiterArticle.getArticleCoAuthors().getAuthors()) {

						boolean isMiddleNameMatch = StringUtils.equalsIgnoreCase(
								reCiterAuthor.getAuthorName().getMiddleInitial(), targetAuthor.getAuthorName().getMiddleInitial());

						boolean isFirstNameMatch = StringUtils.equalsIgnoreCase(
								reCiterAuthor.getAuthorName().getFirstName(), targetAuthor.getAuthorName().getFirstName());

						if (isMiddleNameMatch && isFirstNameMatch) {
							selectedClusterIds.add(entry.getKey());
						}
					}
				}
			}
		}
	}

	/**
	 * Reassign individual articles that are similar to the target author based
	 * on a given instance of strategy context.
	 * @param strategyContexts list of strategy context that are going to be used to reassign the article.
	 * @param clusters current state of the clusters.
	 * @param targetAuthor target author.
	 */
	public void reAssignArticles(List<StrategyContext> strategyContexts, Map<Integer, ReCiterCluster> clusters, TargetAuthor targetAuthor) {
		for (StrategyContext strategyContext : strategyContexts) {
			handleStrategyContext(strategyContext, clusters, targetAuthor);
		}
	}

	/**
	 * Handler for target author specific strategy context.
	 * @param targetAuthorStrategyContext
	 * @param clusters
	 * @param targetAuthor
	 */
	public void handleTargetAuthorStrategyContext(
			TargetAuthorStrategyContext targetAuthorStrategyContext, 
			Map<Integer, ReCiterCluster> clusters, 
			TargetAuthor targetAuthor) {

		// Map of cluster ids to ReCiterarticle objects. Keep tracks of the new cluster ids that these
		// ReCiterArticle objects will be placed at the end of the below loop.
		Map<Integer, List<ReCiterArticle>> clusterIdToReCiterArticleList = new HashMap<Integer, List<ReCiterArticle>>();

		for (int clusterId : selectedClusterIds) {
			for (Entry<Integer, ReCiterCluster> entry : clusters.entrySet()) {
				// Do not iterate through the selected cluster ids's articles.
				if (!selectedClusterIds.contains(entry.getKey())) {

					// Iterate through the remaining final cluster that are not selected in selectedClusterIds.
					Iterator<ReCiterArticle> iterator = entry.getValue().getArticleCluster().iterator();
					while (iterator.hasNext()) {
						ReCiterArticle otherReCiterArticle = iterator.next();

						if (targetAuthorStrategyContext.executeStrategy(otherReCiterArticle, targetAuthor) > 0) {
							if (clusterIdToReCiterArticleList.containsKey(clusterId)) {
								clusterIdToReCiterArticleList.get(clusterId).add(otherReCiterArticle);
							} else {
								List<ReCiterArticle> articleList = new ArrayList<ReCiterArticle>();
								articleList.add(otherReCiterArticle);
								clusterIdToReCiterArticleList.put(clusterId, articleList);
							}
							// remove from old cluster.
							iterator.remove();
						}
					}
				}
			}
		}
		// Now move the selected article to its cluster where the score returns greater than 0
		// using clusterIdToReCiterArticleList map.
		for (Entry<Integer, List<ReCiterArticle>> entry : clusterIdToReCiterArticleList.entrySet()) {
			for (ReCiterArticle article : entry.getValue()) {
				clusters.get(entry.getKey()).add(article);
			}
		}
	}

	/**
	 * Handler for ReCiterArticle specific strategy context.
	 * @param reCiterArticleStrategyContext
	 * @param clusters
	 * @param targetAuthor
	 */
	public void handleReCiterArticleStrategyContext(
			ReCiterArticleStrategyContext reCiterArticleStrategyContext,
			Map<Integer, ReCiterCluster> clusters, 
			TargetAuthor targetAuthor) {

		Map<Integer, List<ReCiterArticle>> clusterIdToReCiterArticleList = new HashMap<Integer, List<ReCiterArticle>>();

		for (int clusterId : selectedClusterIds) {
			for (Entry<Integer, ReCiterCluster> entry : clusters.entrySet()) {
				// Do not iterate through the selected cluster ids's articles.
				if (!selectedClusterIds.contains(entry.getKey())) {
					for (ReCiterArticle reCiterArticle : clusters.get(clusterId).getArticleCluster()) {

						// Iterate through the remaining final cluster that are not selected in selectedClusterIds.
						Iterator<ReCiterArticle> iterator = entry.getValue().getArticleCluster().iterator();
						while (iterator.hasNext()) {
							ReCiterArticle otherReCiterArticle = iterator.next();

							if (reCiterArticleStrategyContext.executeStrategy(reCiterArticle, otherReCiterArticle) > 0) {
								if (clusterIdToReCiterArticleList.containsKey(clusterId)) {
									clusterIdToReCiterArticleList.get(clusterId).add(otherReCiterArticle);
								} else {
									List<ReCiterArticle> articleList = new ArrayList<ReCiterArticle>();
									articleList.add(otherReCiterArticle);
									clusterIdToReCiterArticleList.put(clusterId, articleList);
								}
								// remove from old cluster.
								iterator.remove();
							}
						}
					}
				}
			}
		}

		// Add article to existing cluster.
		for (Entry<Integer, List<ReCiterArticle>> entry : clusterIdToReCiterArticleList.entrySet()) {
			for (ReCiterArticle article : entry.getValue()) {
				clusters.get(entry.getKey()).add(article);
			}
		}
	}

	/**
	 * Handle removal of articles from selected clusters.
	 * @param removeReCiterArticleStrategyContext
	 * @param clusters
	 * @param targetAuthor
	 */
	public void handleRemoveReCiterArticleStrategyContext(
			RemoveReCiterArticleStrategyContext removeReCiterArticleStrategyContext,
			Map<Integer, ReCiterCluster> clusters,
			TargetAuthor targetAuthor) {

		ReCiterCluster clusterOfRemovedArticles = new ReCiterCluster();

		for (int clusterId : selectedClusterIds) {
			Iterator<ReCiterArticle> iterator = clusters.get(clusterId).getArticleCluster().iterator();
			while (iterator.hasNext()) {
				ReCiterArticle reCiterArticle = iterator.next();
				double score = removeReCiterArticleStrategyContext.executeStrategy(reCiterArticle, targetAuthor);
				if (score > 0) {
					clusterOfRemovedArticles.add(reCiterArticle);
					iterator.remove();
				}
			}
		}

		clusters.put(clusterOfRemovedArticles.getClusterID(), clusterOfRemovedArticles);
	}

	/**
	 * Handle removal of a cluster from selected clusters.
	 * @param removeClusterStrategyContext
	 * @param clusters
	 * @param targetAuthor
	 */
	public void handleRemoveClusterStrategyContext(
			RemoveClusterStrategyContext removeClusterStrategyContext,
			Map<Integer, ReCiterCluster> clusters,
			TargetAuthor targetAuthor) {

		Iterator<Integer> iterator = selectedClusterIds.iterator();
		while (iterator.hasNext()) {
			int clusterId = iterator.next();
			ReCiterCluster cluster = clusters.get(clusterId);
			double score = removeClusterStrategyContext.executeStrategy(cluster, targetAuthor);
			if (score > 0) {
				iterator.remove();
			}
		}
	}


	@Override
	public void handleNonSelectedClusters(
			TargetAuthorStrategyContext strategyContext, 
			Map<Integer, ReCiterCluster> clusters, 
			TargetAuthor targetAuthor) {

		// Map of cluster ids to ReCiterarticle objects. A new cluster that contains previously not selected articles.
		List<ReCiterArticle> movedArticles = new ArrayList<ReCiterArticle>();
		for (Entry<Integer, ReCiterCluster> entry : clusters.entrySet()) {
			// Do not iterate through the selected cluster ids's articles.
			if (!selectedClusterIds.contains(entry.getKey())) {

				// Iterate through the remaining final cluster that are not selected in selectedClusterIds.
				Iterator<ReCiterArticle> iterator = entry.getValue().getArticleCluster().iterator();
				while (iterator.hasNext()) {
					ReCiterArticle article = iterator.next();

					if (strategyContext.executeStrategy(article, targetAuthor) > 0) {
						movedArticles.add(article);
						iterator.remove();
					}
				}
			}
		}

		slf4jLogger.info("size=" + movedArticles.size());

		// Create a new cluster containing these articles and put its cluster id into the selectedClusterIds.
		if (movedArticles.size() > 0) {
			ReCiterCluster newCluster = new ReCiterCluster();
			newCluster.setClusterOriginator(movedArticles.get(0).getArticleId()); // select a cluster originator.
			newCluster.setArticleCluster(movedArticles);
			clusters.put(newCluster.getClusterID(), newCluster);
			selectedClusterIds.add(newCluster.getClusterID());
			slf4jLogger.info("new cluster=" + newCluster.getClusterID());
		}
	}


	/**
	 * Handler for a generic StrategyContext object.
	 * @param strategyContext
	 * @param clusters
	 * @param targetAuthor
	 */
	public void handleStrategyContext(StrategyContext strategyContext, Map<Integer, ReCiterCluster> clusters, TargetAuthor targetAuthor) {
		if (strategyContext instanceof TargetAuthorStrategyContext) {
			handleTargetAuthorStrategyContext((TargetAuthorStrategyContext) strategyContext, clusters, targetAuthor);
		} else if (strategyContext instanceof ReCiterArticleStrategyContext) {
			handleReCiterArticleStrategyContext((ReCiterArticleStrategyContext) strategyContext, clusters, targetAuthor);
		} else if (strategyContext instanceof RemoveReCiterArticleStrategyContext) {
			handleRemoveReCiterArticleStrategyContext((RemoveReCiterArticleStrategyContext) strategyContext, clusters, targetAuthor);
		} else if (strategyContext instanceof RemoveClusterStrategyContext) {
			handleRemoveClusterStrategyContext((RemoveClusterStrategyContext) strategyContext, clusters, targetAuthor);
		}
	}

	public Set<Integer> getSelectedClusterIds() {
		return selectedClusterIds;
	}

	public void setSelectedClusterIds(Set<Integer> selectedClusterIds) {
		this.selectedClusterIds = selectedClusterIds;
	}

	public List<StrategyContext> getStrategyContexts() {
		return strategyContexts;
	}

	public StrategyContext getArticleTitleInEnglishStrategyContext() {
		return articleTitleInEnglishStrategyContext;
	}

	public void setArticleTitleInEnglishStrategyContext(StrategyContext articleTitleInEnglishStrategyContext) {
		this.articleTitleInEnglishStrategyContext = articleTitleInEnglishStrategyContext;
	}
}
