package tasktree.aws.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import tasktree.spi.Task;

import java.util.List;
import java.util.stream.Stream;

public class FilterTargetGroups extends AWSFilter<TargetGroup> {
    static final Logger log = LoggerFactory.getLogger(FilterInstances.class);

    private boolean match(TargetGroup resource) {
        var prefix = getAwsCleanupPrefix();
        var match = resource.targetGroupName().startsWith(prefix);
        log.trace("Found Target Group {} {}", mark(match), resource);
        return match;
    }

    protected List<TargetGroup> filterResources() {
        var elb = aws.getELBClientV2(getRegion());
        var resources = elb.describeTargetGroups().targetGroups();
        var matches = resources.stream().filter(this::match).toList();
        log.info("Matched {} Target Groups in region [{}] [{}]", matches.size(), getRegion(), matches);
        return matches;
    }


    @Override
    public void run() {
        var resources = filterResources();
        addAllTasks(deleteTasks(resources));
    }

    protected Stream<Task> deleteTasks(List<TargetGroup> subnets) {
        return subnets.stream().map(this::deleteTask);
    }

    protected Task deleteTask(TargetGroup resource) {
        return new DeleteTargetGroup(getConfig(), resource);
    }
}
