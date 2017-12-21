# Forex OneForge Proxy

## Requirements

> An internal user of the application should be able to ask for an exchange rate between 2 given currencies, and get back a rate that is not older than 5 minutes.
The application should at least support 10.000 requests per day.

## Free account limitations

> One Forge free account supports only 1000 requests per day, 10-20 updates per minute

## Solutions

1. A naive and straightforward proxy that sends each request to `1forge`. 
    - this solution is not acceptable since it does not fit the requirements
2. Use caching mechanism and straightforward proxy: application checks availability of exchange rate in cache and in case of cache miss sends request to `1forge`
    - this solution improves the availability of proxy service but still does not meet the requirements since in worst case quota of 1000 requests will be exhausted after 2 hours, so rest 9000 requests to the proxy will be unsuccessful
3. Use several (10) free `1forge` accounts with solution #1 to meet the requirements
    - the main caveat is that proxy still will be limited by *accounts_number * 1000* requests per day, so without any throttling, the proxy may go to "unavailable" mode quite fast
4. Use caching mechanism like in solution #3 but send *bulk* requests (to fetch all available currency rates at once) to `1forge`
    - without synchronization of cache reads and writes in worst case of high load and high contention this solution may not fit the requirements, since lock-free data structures may repeat cache swap side-effects (requests to `1forge`)
5. Use currency rates cache filled by some scheduled task using *bulk* requests to `1forge`. Requests to proxy are only low-latency cache reads.
6. Combine #3 and #5 solutions, in case if requirement of 5 minute freshness will change

Solution #5 was implemented.

## Improvements

- Define cache-update retry rules
- Grafana alerts in cases of quota exhaustion and cache update failures

## How to run

### Tests

~~~sbtshell
 sbt test
~~~

### Run

- [Optional] start grafana docker container as described in [grafana readme page](./grafana/README.md)
- start application

~~~sbtshell
 sbt run
~~~

- perform some requests like

~~~bash
 curl http://localhost:8888/?from=usd\&to=eur
~~~

- you can use `ab` to perform some load and

~~~bash
 ab -k -n 1000000 -c 50 http://localhost:8888/?from=jpy\&to=eur
 ab -k -n 1000000 -c 50 http://localhost:8888/?from=jpy\&to=usd
~~~

## NOTES:

- perhaps some adjustments with timeouts needed
