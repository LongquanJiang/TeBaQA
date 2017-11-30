package de.uni.leipzig.tebaqa.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SPARQLResultSet {
    private int type;
    private List<String> resultSet;

    public SPARQLResultSet() {
        this.type = -1;
        this.resultSet = new ArrayList<>();
    }

    public SPARQLResultSet(List<String> resultSet) {
        this.type = -1;
        this.resultSet = resultSet.stream().map(s -> s.contains("^^") ? s.split("\\^\\^")[0] : s).collect(Collectors.toList());
    }

    public SPARQLResultSet(List<String> resultSet, int type) {
        this.type = type;
        this.resultSet = resultSet.stream().map(s -> s.contains("^^") ? s.split("\\^\\^")[0] : s).collect(Collectors.toList());
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<String> getResultSet() {
        return resultSet;
    }
}