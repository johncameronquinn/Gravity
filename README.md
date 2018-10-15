# NOTICE #

Android client repository for the former company, Gravity. Designed and developed by John Cameron Quinn. quinnjc@g.cofc.edu

Note that some of the feature and UX decisions were made in conjunction with other members of the company.

----

Hello, and welcome to my work! I have my complaints about the project, looking back. I definitely bit off way, way more than I could chew. But, you live and learn right? Complaints, aside, I'm really proud of this repository. I am proud that I wasn't taught any android development, or indeed, any of the vast variety of skills that are needed to complete a project of this size, yet I still produced something that I believe is quite impressive. Shoutout to stack overflow and the wonderful android documentation that made self-teaching possible to the nth degree. 

As I've been progressing in my career a large amount since this project, I don't consider it a full representation of my skills. For one thing, I wasn't taking code style very seriously. That's definitely not the case anymore. However, I am proud of the scope and depth of this project. Here's a brief overview of the features of our application:

- single-activity UI, with a snapchat-style UX
- multithreaded camera implementation
- multiprocess architecture
- content-provider backed by SQlite database to serve content between processes
- asynchronous image downloading, memory caching, disk caching, and decoding
- asynchronous server connectivity to our own EC2 application server, using HTTPS and token-authentication
- dynamic, static, and analytical content all handled separately
- GCM-enabled messenging and replies
- Development, Staging, and Production Builds
- Resiliency, intelligent error-handling, error-reporting (partially implemented) 

It is important to note that it is in an unfinished state. The major features are there, but, there's a lot of resiliency work that needs to be done. Error-handling, broader testing on devices, etc. Towards the end we were running into issues on the marketing/UX fronts, so a good deal of the UI is unfinished. Plus, there's small UX features like pinch zoom, and whatnot, that weren't yet implemented because we wanted to be sure of the design first. I'd consider it in an alpha state, but of course, some people define that term differently. 

----

I have recently updated the dependencies and enabled a "client-only-mode," where posted content loops back into the device storage, since our servers are no longer operational. However, this mode was depreciated in an earlier version so effort will need to be made to make it functional once more. Truthfully, so much of the application's function is a disabled without servers, that a client-only test is lacking in some ways. However, a full demonstration could be implemented on request.
