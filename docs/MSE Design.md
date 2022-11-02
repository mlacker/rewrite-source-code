# Metadata Storage Engine System Design

## Functional Requirements

- 支持版本，保留历史设计的版本，可以回滚到指定版本；
- 支持分支，模块设计是由多用户协作开发，用户有自己独立的分支；
- 分支合并，用户可以把自己的设计合并到主干分支，并处理冲突；
- 应用分发，模块可以被 fork，产生新的模块；
- 模块依赖，模块可以依赖其它模块的指定版本；
- 实时保存，客户端产生的变更能即时持久化，避免数据丢失；

## Non-Functional Requirements

- High Availability
  No single point failure
- Scalability
  Easy to scale for large amount of requests
- Low Latency
  For both write and read, latency should be as low as possible
- Eventual Consistency
- Durability
  Data loss is NOT acceptable
- Write Heavy
  Read : Write = 10 : 100

## Assumptions

1k TPS

Module size: 800 KB
Metadata size: 400 Bytes
Element's = 800 KB / 400 B = 2,000 count

records: 1k TPS \* 10% \* 365 \* 86400s => 3.156e+10 => 3.15 GB rows per year
Stroage size: 1k TPS * 400 B => 1.2 TB per year

## Define API

staging(moduleId: Long, userId: Long, elementId: Long, metadata: String)
commit(moduleId: Long, userId: Long): Long
merge(moduleId: Long, userId: Long)

load(moduleId: Long, userId: Long): Module

```sql
`commit`:
  id, prev, committed_user, committed_time

`element`:
  id, commit_id, element_id, metadata

`staging`:
  module_id, user_id, element_id, metadata

`branch`:
  id, module_id, user_id, head

```

## High Level Design

## Low Level Design

### version

### snapshot

#### 版本快照

当前版本包含哪些元素

#### 数据快照

当前版本的模块数据快照

#### 快照策略

通过衰减均值统计每个 branch.head 的访问频率，用路径算法生成每个 commit 的频率。
根据 commit 的频率和重建开销来决策，哪些 commit 应该生成快照。

## Summary

### 性能提升

### 性能压测

### 性能瓶颈
