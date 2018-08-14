# Rivers是什么?
    Rivers是一个支持在任意类型数据源之间进行可定时定量交换数据的中间件。 


![image](https://github.com/fnOpenSource/rivers/blob/master/architectures.png)

# Rivers用来解决什么?

目前成熟的数据交换工具比较多，但是一般都只能用于数据导入或者导出，并且只能支持几个特定类型的数据库扩展性差，对复杂多样化的业务需求往往也无法满足。

这样带来的一个问题是，为满足多样化业务需求，需要进行大量的业务逻辑开发和匹配不多数据源，带来项目的不稳定性和庞大的无法维护的代码，而且以后每增加一种库类型，我们需要掌握和部署的工具数量将线性增长。
数据交换的任务中常见的需求，比如定时增量全量导入、数据简单转化、数据简单检索需求、分布式数据处理以及小范围的搜索需求等。
Rivers正是为了解决这些问题而生。 


==>>[详细文档参照wiki](https://github.com/fnOpenSource/rivers/wiki)  
