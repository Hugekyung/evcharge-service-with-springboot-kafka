# F3 cleanup receipt

The Java app was interrupted after the probes. The console consumer exited after its bounded timeout. Kafka was restarted with `docker compose start kafka`; PostgreSQL and Kafka both reported `healthy`. No `docker compose down`, volume deletion, or destructive cleanup was run.
