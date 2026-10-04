I want to vibe-write a new app. It must be stored in this directory.

the app will be a KMP app, with targets of Desktop (Mac, Windows).

Make sure you read the AGENTS.md file.

The idea is for the app to be a database of toys. 

There will be the following tables to begin with:
    - Makers - that is the table that represents the manufacturers
    - slots - the slot car collection
    - trains - the trains collection
    - static - static models collection
    - kits - Kits collection
    - misc - Others

There is some subdirectories in this project's directory:
    - images - this is where images and sound files are stored
    - json - json files to be imported, corresponding to the tables.

There is documentation of a future schema in SCHEMA.md
    
If you need resources icons, bitmaps & etc, you can use the Icons from the ToyCollectionMultiPlatform

The app needs to have CRUD to maintain those tables, as well as import those jsons to initialize the DB, and export them.

There is a legacy application that generated those json files. For the forseeable future, the legacy application
will continue to be used, exporting the json files, importinng in this new app. Therefore, strict compliance with the schema of those json is mandatory.

Toys have a main picture, but we don't have a field for main picture. The way it works, is if the refnum of a slot car is "1234", and its picture field is a 'y' , it means this slot car
has a main picture, and the display software will look for an image file named "car1234.*". The schema has a category settings table. As of now we have only 5 categories, but
the app should allow for creating new categories or deleting them.
The values being used today for the types of toys is this:
 - slot cars use 'car'
 - trains use 'tra'
 - static models use 'sta'
 - kits use 'pla'
 - Others use 'mis'.


You can take code if you need from the ToyCollectionMultiplatform, if you need an idea how to layout the pages, and etc. But you don't need to use that layout. It is important to have
a robust search feature, you can use it from that app, and enhance it as you see fit.

You must provide a detail plan, and store that plan in here.