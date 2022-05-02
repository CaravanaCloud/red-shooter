package cloudjanitor.aws.ec2;

import cloudjanitor.TaskTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.services.ec2.model.Vpc;

import javax.inject.Inject;
import javax.swing.text.html.Option;

import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static java.util.concurrent.TimeUnit.*;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeleteEmptyVPCTest extends TaskTest {

    @Inject
    CreateVPC createVPC;

    @Inject
    FilterVPCs filterVPCs;

    @Inject
    DeleteVPCs deleteVpc;

    @Test
    public void testCreateAndDeleteVPC(){
        var vpcId = createVPC();
        awaitCreate(vpcId);
        assertTrue(vpcExists(vpcId));
        deleteVPC(vpcId);
        awaitDelete(vpcId);
        assertFalse(vpcExists(vpcId));
    }

    private void awaitCreate(String vpcId) {
        await().atMost(30, SECONDS)
                .until(() -> vpcExists(vpcId));
    }

    private void awaitDelete(String vpcId) {
        await().atMost(30, SECONDS)
                .until(() -> ! vpcExists(vpcId));
    }

    private void deleteVPC(String vpcId) {
        deleteVpc.filterVPCs.targetVpcId = Optional.of(vpcId);
        tasks.runTask(deleteVpc);
    }

    @SuppressWarnings("unchecked")
    private boolean vpcExists(String vpcId) {
        filterVPCs.targetVpcId = Optional.of(vpcId);
        tasks.runTask(filterVPCs);
        var vpcs = (List<Vpc>) filterVPCs.findAsList("aws.vpc.matches");
        if (! vpcs.isEmpty()){
            var vpcExists = vpcs.get(0).vpcId().equals(vpcId);
            return vpcExists;
        } return false;
    }

    private String createVPC() {
        var vpcId = tasks
                .runTask(createVPC)
                .findString("aws.vpc.id");
        return vpcId.orElse(null);
    }

}