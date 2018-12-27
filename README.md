

## What does it do?
The application requests moscow public transport schedule from the public sources (like Mosgortrans website). The main idea was to create a user-friendly interface to the schedule, routes, stops, etc.

## What is done?
* Fetching schedule from 2 sources:
  * Mosgortrans http://www.mosgortrans.org/pass3/
  * Moscow transport http://lkcar.transport.mos.ru
* Caching schedule on the disk
* Managing stops, routes, directions etc in a general way
* Allowing user to save favourite stops on the main activity
* Nice schedule view
* Widget for the home screen with the simplified schedule for selected favourite stop

## Which transport types are supported?
Basically, everything (excluding underground):
* Bus
* Tram
* Trolley
