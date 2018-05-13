# NOTICE #

Android client repository for the former company, Gravity. Designed & Developed by John Cameron Quinn. quinnjc@g.cofc.edu

Note that some of the feature and UX decisions were made inconjunction with other members of the company.

--- Project Overview and Commentary --- 

  The state of this repository is early alpha. It was intended to be competitive with other enterprise-level applications such as Snapchat. When I was working on this client I was also managing the company as a whole, and concessions had to be made to find the time.
  
   Because of this divide, the most lacking aspect of this project is code quality and style. To say it succiently, some of it is pretty bad code. At the beginning we didn't have the requisite skills to complete this project, so we learned them. However, this means that to create 'good code' I would have had to rewrite repeatedly, and considering the project's scope I decided to leave that much for later. Often I was testing things I didn't fully understand, toying with various implementations, etc. As I progressed as a developer my code quality improved because the amount of rewrites I had to do decreased, but I neglected to contatntly update and maintain quality. Athough this would not be acceptable normally, as the sole contributor in a proprietary closed-source setting I made this sacrifice to buy time.
 
  Although the style is lacking, the architecture and features of this project are very advanced. I prioritized user-experience above all else in my decision-making. Features and UI were designed to be comparable to Snapchat, albiet a very early Snapchat, but good enough to achieve what we needed. Major aspects of the project include a multithreaded Camera implementation, GCM-enabled replies, memory and disk caching, SQLite database storage, multiprocess architecture, and a fully fragment-based user interface. Content was divided into analytics, dynamic, and static content. All were handled in significantly different manners. Efforts were made for the main process to handle and fix its own errors, to avoid crashing at all costs, and to advise the user on how to fix issues that arise that are outside of the application's control - eg. Camera repeatedly failed to connect. Effort was made for the application to support the full array of android devices on the market, however, some aspects such as the Camera api are manufacturer or even device-specific, which means support can only be verified by testing using the physical device in question. 
  
  Amazon AWS served as our IAAS choice for the company. We utilized their services: CloudFront, S3, Route53, Elastic Load-Balancer, SES, SNS, EC2, and others to create 'our servers'. They were designed to handle up to 10k users at launch, with the ability to rapidly scale to hundreds of thousands more without sacrificing uptime.

----

I have recently updated the dependencies and enabled a "client-only-mode," where posted content loops back into the device storage. However, this mode was depreciated in an earlier version so effort will need to be made to make it functional. Truthfully, so much of the application's function is a result of the server that a client-only test is lacking in some ways. However, a full demonstration could be implemented on request. 
