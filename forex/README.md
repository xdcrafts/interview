# Forex OneForge Proxy

## Assumptions

Since proxy should support at least 10000 requests per day, rates should not be older than 5 minutes
and one forge free account allows only 1000 request per day with at least 10 updates per minute
we have next options:

- Use several one forge free accounts with load balancing and proxy each request directly
    - pros
        - always most recent data
    - cons
        - proxy will be limited by *accounts_number * 1000* requests per day, so without any throttling proxy may go to "unavailable" mode
- Since one forge api supports bulk requests we can use single one forge account per service instance and use in-memory cache for rates
    - sync: on proxy api call we may check timestamp of cached rate and perform bulk update if needed
        - pros
            - allows to minimize api calls to one forge
        - cons
            - latency of some requests may be greater than with async option
            - high contention scenario may cause unnecessary calls to one forge api and quota can be exhausted quite fast
    - async: fill some in-memory cache with one forge data by some periodically task
        - pros
            - best availability
            - best latency
        - cons
            - unnecessary api calls to one forge may happen
            - one forge api errors are hidden from user, monitoring required
- Kind of a both options at the same time in case if requirement of 5 minute freshness will change

## How to run

To run tests use

  ~~~
  sbt test
  ~~~

To run application just use

  ~~~
  sbt run
  ~~~

To choose implementation, navigate to *forex.main* package object and switch *OneForgeImpl* type to

- OneForgeLive for live solution with asynchronous updates
- OneForgeDummy for dummy implementation

## NOTES:

- perhaps some adjustments with timeouts needed
- motivation was
    - to preserve interfaces and contracts between modules
    - to build asynchronous immutable cache of values
