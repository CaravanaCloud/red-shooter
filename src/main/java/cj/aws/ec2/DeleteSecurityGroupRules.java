package cj.aws.ec2;

import cj.Input;
import cj.Output;
import cj.aws.AWSTask;
import cj.spi.Task;
import software.amazon.awssdk.services.ec2.model.SecurityGroupRule;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@Dependent
public class DeleteSecurityGroupRules extends AWSTask {
    @Inject
    FilterSecurityGroupRules filterRules;

    @Inject
    Instance<DeleteSecurityGroupRuleTask> deleteRuleInstance;
    @Override
    public Task getDependency() {
        return filterRules;
    }

    @Override
    public void apply() {
        var rules = filterRules.outputList(Output.AWS.SecurityGroupRulesMatch, SecurityGroupRule.class);
        rules.forEach(this::deleteRule);
    }

    private void deleteRule(SecurityGroupRule sgr) {
        var deleteRule = deleteRuleInstance.get()
                .withInput(Input.aws.securityGroupRule, sgr);
        submit(deleteRule);
    }

}
