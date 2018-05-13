# NOTICE #

Android client repository for the former company, Gravity. Designed and developed by John Cameron Quinn. quinnjc@g.cofc.edu

Note that some of the feature and UX decisions were made in conjunction with other members of the company.

--- Project Overview and Commentary --- 

  The state of this repository is early alpha. It was intended to be competitive with other enterprise-level applications such as Snapchat. Please note that I was designing and developing this app, as well as leading our business planning and development, simultaneously.
  
   Because of this divide, the most lacking aspect of this project is code quality and style. To say it succinctly, some of it is pretty bad code. At the beginning we didn't have the requisite skills to complete this project, so we learned them. However, this means that to create good code I would have had to rewrite repeatedly and considering the project's scope I decided to leave that much for later. The code quality does increase significantly over time as the number of requisite rewrites decreased, but old code was not updated. Although this would not be acceptable normally, as the sole contributor in a proprietary closed-source setting, I made this sacrifice to buy time. This is part of the litany of decisions that comes with the territory of bringing a company from concept to operation.
 
  Although the style is lacking, the architecture and features of this application are competitive. I prioritized user-experience above all else in my decision-making. Features and UI were designed to be comparable to Snapchat, albeit a very early Snapchat, but good enough to achieve what we needed. Major aspects of the project include a multithreaded Camera implementation, GCM-enabled replies, memory and disk caching, SQLite database storage, multiprocess architecture, and a fully fragment-based user interface. Content was divided into analytics, dynamic, and static content. All were handled in significantly different manners. Efforts were made for the main process to handle and fix its own errors, to avoid crashing at all costs, and to advise the user on how to fix issues that arise that are outside of the application's control – e.g. Camera repeatedly failed to connect. Effort was made for the application to support the full array of Android devices on the market, however, some aspects such as the Camera API are manufacturer or even device-specific, which means support can only be verified by testing using the physical device in question. 
  
  AWS served as our IaaS choice for the company. We utilized their services (CloudFront, S3, Route53, Elastic Load Balancer, SES, SNS, EC2, and others) to create our application service. They were designed to handle up to 10k users at launch, with plans to scale to many thousands more without sacrificing uptime.

----

I have recently updated the dependencies and enabled a "client-only-mode," where posted content loops back into the device storage, since our servers are no longer operational. However, this mode was depreciated in an earlier version so effort will need to be made to make it functional once more. Truthfully, so much of the application's function is a disabled without servers, that a client-only test is lacking in some ways. However, a full demonstration could be implemented on request.
