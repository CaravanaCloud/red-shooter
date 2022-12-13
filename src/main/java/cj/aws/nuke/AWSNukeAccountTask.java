package cj.aws.nuke;

import cj.TaskDescription;
import cj.TaskMaturity;
import cj.TaskTemplate;
import cj.aws.AWSInput;
import cj.aws.AWSTask;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

import static cj.TaskMaturity.Level.experimental;

@Dependent
@Named("aws-nuke-account")
@TaskMaturity(experimental)
@TaskDescription("Runs aws-nuke for a single account")
@TaskTemplate(value="aws-nuke.qute.yaml", output="aws-nuke.yaml")
@SuppressWarnings("unused")
public class AWSNukeAccountTask extends AWSTask {
    @Override
    public void apply() {
        var accountId = getInputString(AWSInput.accountId);
        debug("aws-nuke started for account {}", accountId);
        runNuke();
        debug("aws-nuke completed.");
    }

    private void runNuke() {
        var dryRun = getInputString(AWSNukeInput.dryRunFlag);
        var cmd = new String[]{
                "aws-nuke"
                , dryRun
                ,"--force"
                ,"--config"
                , taskFile("aws-nuke.yaml").toString()
        };
        checkpoint("Executing aws-nuke: {}",
                String.join(" ", cmd));
        shell().exec(cmd);
    }
}
