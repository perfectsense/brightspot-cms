Welcome to Brightspot
=========================

[www.brightspot.com/docs](http://brightspot.com/docs)

**What is Brightspot?**

Brightspot is an enterprise user experience platform, designed and developed to power large-scale, highly dynamic, editorially rich and visually stunning consumer experiences. Brightspot brings teams together from day one and speeds up their day-to-day design, development and editorial processes.

**Unleash designers:** create visually-stunning experiences with complete design ﬂexibility and zero platform constraints.
 
**Empower developers:** automate routine tasks and inject industry best practices in development workﬂows they already know and understand. Brightspot offers the Dari developer framework toolset to speed up development.
 
**Inspire editors:** make the publishing tool as compelling and as easy-to-use as the  experience it powers.
 
**Enable collaboration:** facilitate immediate partnership between teams with rapid prototyping, instant changes, and automated tool chains.

Brightspot was built using the [Dari Framework](http://github.com/perfectsense/dari), a powerful object persistence library that makes it easy to build complex content types and persist them to one or more database backends. 

It enables the creation of complex data models. Using simple annotations and standard Java types, anything can be modeled and accessed with a web view, or through an API. As a user experience platform, Brightspot can be used as a publishing platform (CMS) for websites and native mobile applications, an application back-end, CRM and much more.

An example Brightspot content model:

```
public class MyClass extends Content {
   
	private String textField;
	private Date dateWidget;
	private List<String> textList;
       
}
```

![](http://d3qqon7jsl4v2v.cloudfront.net/25/8f/eb630e7b4270a072e6b35c1d317d/screen-shot-2014-12-03-at-120246-pmpng.32.11%20PM.png)

A user interface for publishing data is built automatically. New fields instantly show up within the Brightspot interface, ready for data to be input by the user. Brightspot comes with a library of field types that can be used to create this interface.


**How does it work?**

Brightspot can be used to build any type of Java application. The difference between a standard Java object and Brightspot is the extension of `Content`, a parent class that Brightspot provides:

```
public class MyClass extends Content {
   
	private String textField;
	private Date dateWidget;
	private List<String> textList;
       
}
```

By extending Content Brightspot knows to create a user interface with the Class, and persist the object in the database as a Content Type, as well as any instances of it.

All Content in Brightspot extends the Dari Record class: `com.psddev.dari.db.Record`. Objects are not mapped to database tables, instead they are serialized into JSON and stored in the database as JSON. This frees developers from worrying about creating or altering database tables and allows for rapid evolution of data models.

```
{
  "textField" : "Create a user interface automatically",
  "dateWidget" : 1393688100000,
  "textList"; : [ "Standard Java" ],
}
```

All fields in a class that extends Content / Record are persisted to the database when the object is saved.

Developers are given complete control over how their data is stored, and they can change this at any time, without database updates needed.

Here is an example data model of an Author content type:

```
public class Author extends Content {
    
    private String name;
    private String bio;

    // Getters and Setters
}
```

![](http://d3qqon7jsl4v2v.cloudfront.net/b1/89/249636264cf896aa62aca89404fc/screen-shot-2014-12-03-at-121112-pmpng.33.29%20PM.png)

Within Brightspot the Author content type can immediately be created, saved, drafted, scheduled, and searched for. It can be versioned, changes in the content between versions can be tracked, and roles and permissions for access to the creation of an Author can be applied.

Brightspot uses Dari to save objects to two databases, SQL and Solr.

**How is it different?**

The custom creation of content types using Java classes means each instance of Brightspot is tailored for the user. Labels, terms and content types are all based on the application being created. Brightspot, in this sense, is a platform on which tailored applications are developed. The platform provides features that every application can use, such as search, scheduling and publishing to the web. Brightspot doesn't dictate what is managed in the platform, or how it is presented on the web.

**How do I start using it?**

The Brightspot stack consists of proven open-source software: Java, MySQL, Solr and Apache httpd. There is nothing proprietary about the technology being used. Brightspot is open-source and freely available for use under the `GNU GENERAL PUBLIC LICENSE V2`.

Java developers can get started with Brightspot development quickly and easily. Brightspot applications are built with standard Java development techniques.

Start using Brightspot by first [installing the platform](http://www.brightspot.com/docs/3.0/overview/installation).

License
=======
Brightspot is released under the GPLv2 license.

Questions / Bugs?
=====

Feel free to submit any questions,  bug reports or feature requests to the
[GitHub Issues](https://github.com/perfectsense/brightspot-cms/issues).
