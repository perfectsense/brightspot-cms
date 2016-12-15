.. code-block:: xml

   <?xml version='1.0' encoding='utf-8'?>
   <Context allowLinking="true" crossContext="true">

   <WatchedResource>WEB-INF/web.xml</WatchedResource>

   <!-- CMS Login Control -->
   <Environment name="cms/tool/isAutoCreateUser" override="true" type="java.lang.Boolean" value="true" />
   <Environment name="cookieSecret" type="java.lang.String" value="Deem2oenoot1Ree5veinu8" override="true" />


   <!-- Default Database -->
   <Environment name="dari/defaultDatabase" type="java.lang.String" value="DATABASE_NAME" override="false" />
   <Environment name="dari/database/DATABASE_NAME/class" override="false" type="java.lang.String" value="com.psddev.dari.db.AggregateDatabase" />
   <Environment name="dari/database/DATABASE_NAME/defaultDelegate" override="false" type="java.lang.String" value="sql" />
   <Environment name="dari/database/DATABASE_NAME/delegate/sql/class" override="false" type="java.lang.String" value="com.psddev.dari.db.SqlDatabase" />

   <Resource name="dari/database/DATABASE_NAME/delegate/sql/dataSource"
   auth="Container" driverClassName="com.mysql.jdbc.Driver"
   logAbandoned="true" maxActive="100" maxIdle="30" maxWait="10000"
   type="javax.sql.DataSource"
   removeAbandoned="true" removeAbandonedTimeout="60"
   username="DATABASE_USER" password="DATABASE_PASS"
   url="jdbc:mysql://localhost:3306/DATABASE_NAME"
   testOnBorrow="true" validationQuery="SELECT 1"/>

   <!-- Solr Config -->
   <Environment name="solr/home" override="false" type="java.lang.String" value="/TOMCAT_PATH/solr" />
   <Environment name="dari/database/DATABASE_NAME/delegate/solr/groups" override="false" type="java.lang.String" value="-* +cms.content.searchable" />
   <Environment name="dari/database/DATABASE_NAME/delegate/solr/class" override="false" type="java.lang.String" value="com.psddev.dari.db.SolrDatabase" />
   <Environment name="dari/database/DATABASE_NAME/delegate/solr/serverUrl" override="false" type="java.lang.String" value="http://localhost:8080/solr" />

   <Environment name="dari/defaultStorage" type="java.lang.String" value="local" override="false" />
   <Environment name="dari/storage/local/class" override="false" type="java.lang.String" value="com.psddev.dari.util.LocalStorageItem" />
   <Environment name="dari/storage/local/rootPath" override="false" type="java.lang.String" value="/TOMCAT_PATH/webapps/media" />
   <Environment name="dari/storage/local/baseUrl" override="false" type="java.lang.String" value="http://localhost:8080/media" />

   </Context>