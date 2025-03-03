# How to Repair the Lucene index

While using lucene we are used to a very high reliability. However, there may come a situation that your index is corrupted

You may see a server error like this:

```
2025-03-03 11:47:31,483 SEVERE [org.imixs.workflow.engine.lucene.LuceneSearchService] (ServerService Thread Pool -- 82) Lucene index error: checksum failed (hardware problem?) : expected=1f461af3 actual=63ace331 (resource=BufferedChecksumIndexInput(MMapIndexInput(path="/opt/jboss/lucene/index/_pmol.cfs") [slice=_pmol.fnm]))
....
```

In this situation you can use the Lucene repair tool `checkindex`.
Checkindex is a tool available in the lucene library, which allows you to check the files and create new segments that do not contain problematic entries. This means that this tool, with little loss of data is able to repair a broken index, and thus save you from do the full indexing of all documents with the Imixs-Admin Tool.

You need to make sure that the lucene core library is available on your machine. Then you can call the following command:

```
-cp lucene-core-7.7.3.jar -ea:org.apache.lucene... org.apache.lucene.index.CheckIndex /opt/jboss/lucene/my-index
```

Make sure the version matches your installation of Imixs-Workflow
