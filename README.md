H2 Auto scalable (AWS) cluster to work with topics from kafka cluster:
H1  Working project.....

H3 Master responsabilities:

1.  Verify new topics
2.  Register new workers(nodes) 
3. Ask workers  about load , memory usage , and network
4.  Use the node info to decide where to send a new job
5.  Supervisor of workers
6.  Save configuration about how to partition the data provided by kafka. For example,  If the message has a date field it can divide the message by hour
or  by directory partition  like  /date/client/product
7. Rest api to config a topic. Ex: (MaxMessageInFlight, how to partition)
8. Dynamic change configuration,  stop worker,  and begin with new configuration from the offset
9. 
  1.  Should notify if the cluster is in heavy use
  2.  Show launch new instances if needed.
10.  Should shutdown instances if able to
11.  Should be able to register a worker for a specific topic (For example if some topic is already being send to s3,  we should be able to register a node and ask for the same topic so the worker can save it on cassandra for example)
12.  The master should be able to recover from a crash
13.  Where to save the configuration?   Every master with a liteSql  and duplicate?

# H2Workers:

1.  should be able to receive the messages from a topic,  and save it to s3 (future (cassandra,  elastic search))
2.  Should receive from the master how to partition the data,  and how to commit
3.  Should be able to tell the master that the lag is getting higher, so the master can try to set up a new node
4.  Order file by some atribute

#H3 Inspiration:
1.  Secor
2.  Bifrost
3.  Camus

# H3ROADMAP:

Version 0.1 ->  master finding topics,  registering workers, ask workers to process topic, save file with size X, compress and send to s3

start Master
start Worker
start Singleton

In SBT, just run ```docker:publishLocal``` to create a local docker container. 

To launch the first node, which will be the seed node:

```
$ docker run -i -t --rm --name seed kuhnen/processor:0.1
```

To add a member to the cluster:

```
$ docker run --rm --name c1 --link seed:seed -i -t kuhnen/processor:0.1
```

