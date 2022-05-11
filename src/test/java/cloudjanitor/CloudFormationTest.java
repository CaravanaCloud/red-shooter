package cloudjanitor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudformation.model.*;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CloudFormationTest extends TaskTest{
    @Inject
    Logger log;

    String stackName;

    @BeforeAll
    public void beforeALl(){
        createStack();
    }

    @AfterAll
    private void afterAll(){
        deleteStack();
    }

    protected void createStack(){
        log.info("Creating stack {} on {}", getStackName(), aws().getRegion());
        var cf = aws().getCloudFormationClient();
        var stackName = getStackName();
        var createReq = CreateStackRequest.builder()
                .stackName(getStackName())
                .capabilities(Capability.CAPABILITY_IAM)
                .templateBody(templateBody(getSimpleName()))
                .build();
        var stackId = cf.createStack(createReq).stackId();
        var waiting = false;
        do {
            var describeReq = DescribeStacksRequest
                    .builder()
                    .stackName(stackName)
                    .build();
            var stacks = cf.describeStacks(describeReq)
                    .stacks();
            if (! stacks.isEmpty()){
                var stack = stacks.get(0);
                var status = stack.stackStatus().toString();
                waiting = switch(status) {
                    case "CREATE_COMPLETE",
                            "CREATE_FAILED",
                            "ROLLBACK_COMPLETE" -> false;
                    default -> true;
                };
                System.out.println("Stack "+stackName+" is "+status);
                if (waiting){
                    System.out.println("Waiting create...");
                    try {
                        Thread.sleep(15_000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                System.out.println("Stack not found");
            }
        } while (waiting);
        System.out.println("CREATE DONE");
    }

    protected String getStackName() {
        if (stackName == null) {
            var simpleName = getSimpleName();
            var runId = config().getExecutionId();
            stackName = simpleName+"-"+runId;
        }
        return stackName;
    }

    private String templateBody(String templateName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        var resourceName = "cloudformation/"+templateName+".yaml";
        InputStream is = classloader.getResourceAsStream(resourceName);
        if(is == null){
            fail("Resource not found: "+resourceName);
            throw new RuntimeException("Resource not found: "+resourceName);
        }else {
            String body = null;
            try {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return body;
        }
    }

    private String getSimpleName() {
        return this.getClass().getSimpleName();
    }

    protected void deleteStack(){
        var cf = aws().getCloudFormationClient();
        var stackName = getStackName();
        var deleteReq = DeleteStackRequest.builder()
                .stackName(stackName)
                .build();
        cf.deleteStack(deleteReq);
        var waiting = false;
        do {
            var describeReq = DescribeStacksRequest
                    .builder()
                    .stackName(stackName)
                    .build();
            try {
                var stacks = cf.describeStacks(describeReq)
                        .stacks();
                if (!stacks.isEmpty()) {
                    var stack = stacks.get(0);
                    var status = stack.stackStatus().toString();
                    waiting = switch (status) {
                        case "DELETE_COMPLETE",
                                "DELETE_FAILED",
                                "ROLLBACK_COMPLETE" -> false;
                        default -> true;
                    };
                    System.out.println("Stack " + stackName + " is " + status);
                    if (waiting) {
                        System.out.println("Waiting delete...");
                        try {
                            Thread.sleep(15_000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Stack not found");
                    waiting = false;
                }
            }catch(CloudFormationException ex){
                System.out.println("Failed to describe stack, consider it gone.");
                waiting = false;
            }
        } while (waiting);
        System.out.println("DELETE DONE");
    }

    protected Optional<String> output(String name) {
        var cf = aws().getCloudFormationClient();
        var stackName = getStackName();
        var req = DescribeStacksRequest.builder()
                .stackName(stackName)
                .build();
        var stacks = cf.describeStacks(req).stacks();
        if (! stacks.isEmpty()) {
            var stack = stacks.get(0);
            var outs =
                    stack.outputs()
                            .stream()
                            .toList();
            var out = outs.stream()
                            .filter(output -> output.outputKey().equals(name))
                            .map(output -> output.outputValue())
                            .findAny();
            return out;
        }
        return Optional.empty();
    }

    protected String getOutput(String name) {
        var out = output(name);
        if (out.isEmpty()){
            fail("Expected output name " + name + " not found in stack "+getStackName());
        }
        return out.orElseThrow();
    }

}