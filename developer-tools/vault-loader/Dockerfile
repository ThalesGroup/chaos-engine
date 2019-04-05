FROM vault:latest
WORKDIR /vault-loader/
ADD ./vault-* ./
RUN touch ./vault-secrets.json

ENV VAULT_ADDR=http://vault:8200/
ENV VAULT_TOKEN=00000000-0000-0000-0000-000000000000
ENTRYPOINT [ "/bin/sh", "-c" ]
CMD [ "./vault-init.sh" ]