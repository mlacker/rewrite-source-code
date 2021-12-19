package com.mlacker.samples.netflix.eureka.cluster

import com.netflix.eureka.util.batcher.TaskProcessor

class ReplicationTaskProcessor : TaskProcessor<ReplicationTask> {

    override fun process(task: ReplicationTask): TaskProcessor.ProcessingResult {
        TODO("Not yet implemented")
    }

    override fun process(tasks: MutableList<ReplicationTask>): TaskProcessor.ProcessingResult {
        TODO("Not yet implemented")
    }
}