const path = require('path');

const requestTypes = {
    HTTP: 'HTTP',
    AMQP: 'AMQP'
};


module.exports = {
    htBackendBaseUrl: "http://v2-beta-external.hypertest.co:8001",
    serviceIdentifier: "1417d1de-25e9-4888-b0f5-4673cc7c0657",
    appStartCommand: 'mvn',
    appStartCommandArgs: [
//         "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005",
//       "-Dspring-boot.run.jvmArguments=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED",
        "spring-boot:run",
    ],
    appWorkingDirectory: path.resolve(__dirname, ''),
    appStartTimeoutSec: 30000,
    requestTypesToTest: [
        requestTypes.HTTP,
    ],
    httpCandidateUrl: 'http://localhost:8080',
    showAppStdErrLogs: true,
    showAppStdOutLogs: true,
    shouldReportHeaderDiffs: false,
    testBatchSize: 10,
    htMockCliDirectory: process.env.HT_MOCK_CLI_DIRECTORY,
    htExtraHeaders: {
        authorization: 'Basic ' + Buffer.from('HyperTest-Demo:HyperTest-Demo').toString('base64'),
    },
    httpReqsToTest: [808152]
};
