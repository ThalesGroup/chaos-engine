FROM vault:latest
WORKDIR /vault-loader/
ADD ./vault-* ./
RUN touch ./vault-secrets.json

ENTRYPOINT [ "/bin/sh", "-c" ]
CMD [ "./vault-init.sh" ]