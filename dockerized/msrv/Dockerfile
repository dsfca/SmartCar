FROM payara/server-web

COPY manufacturer-1.0.war $DEPLOY_DIR
COPY --chown=payara cacerts.jks /opt/payara/appserver/glassfish/domains/domain1/config
COPY --chown=payara files_payara/* /opt/payara
