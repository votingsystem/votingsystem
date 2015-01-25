class PollResultsGeneratorJob {

    static triggers = {
        //http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
        cron name:'cronTrigger', startDelay:10000, cronExpression: '0 0 0 ? * 00:00am MON-SUN ' //00:00am every day
    }

    def EventVSElectionService

    def execute() {
        EventVSElectionService.generateBackups()
    }


}
