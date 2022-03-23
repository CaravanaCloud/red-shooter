package tasktree.visitor;

import tasktree.Result;
import tasktree.ResultType;
import tasktree.spi.Task;

import java.util.*;

public abstract class TaskVisitor implements Visitor {
    Map<ResultType, List<Result>> resultsMap = new HashMap<>();

    protected void collect(Result result){
        if (result == null) return;
        var resultType = result.getType();
        var results = resultsMap.getOrDefault(
                resultType,
                new ArrayList<>());
        results.add(result);
        resultsMap.put(resultType, results);
    }

    protected List<Result> getResults(ResultType type){
        return resultsMap.get(type);
    }

    public List<Result> getResults(){
        var results = resultsMap.values().stream()
                .flatMap(Collection::stream)
                .toList();
        return results;
    }
}