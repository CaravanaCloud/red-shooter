package cj.aws.ec2;

import cj.BaseTask;
import cj.Input;
import cj.aws.AWSIdentity;
import cj.aws.DefaultAWSIdentity;
import cj.aws.sts.LoadAWSIdentitiesTask;
import cj.spi.Task;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import static cj.Input.aws.identity;
import static cj.Output.AWS.Identities;

@Named("cleanup-aws")
@Dependent
public class CleanupAWS extends BaseTask {
    @Inject
    LoadAWSIdentitiesTask loadIdsTask;

    @Inject
    Instance<CleanupAWSIdentity> cleanupAWSIdentityInstance;

    private void setDefaultIdentity() {
        var identity = DefaultAWSIdentity.of();
        getInputs().put(Input.aws.identity, identity);
    }

    @Override
    public void apply() {
        var ids = loadIdsTask.outputList(Identities, AWSIdentity.class);
        debug("Cleaning up {} AWS identities. {}", ids.size(), ids);
        var tasks = ids.stream().map(this::toTask).toList();
        submitAll(tasks);
    }

    private Task toTask(AWSIdentity awsIdentity) {
        return cleanupAWSIdentityInstance.get()
                .withInput(identity, awsIdentity);
    }

    @Override
    public Task getDependency() {
        return loadIdsTask;
    }
}
