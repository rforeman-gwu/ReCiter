package database.dao;

import java.util.List;

import database.model.Analysis;

public interface AnalysisDao {

	void emptyTable();
	void insertAnalysisList(List<Analysis> analysisList);
}