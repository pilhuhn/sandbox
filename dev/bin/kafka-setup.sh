#!/bin/bash

########
# Full setup of test Managed Kafka instance and service accounts.
# It is idempotent and can be run multiple times.
#
# Env vars:
# - MANAGED_KAFKA_INSTANCE_NAME: set the managed kafka instance name (required)
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

. "${SCRIPT_DIR_PATH}/configure.sh" kafka

# rhoas login
function rhoas_login {
  rhoas_logged_in=$( rhoas kafka list &> /dev/null && echo -n "yes" || echo -n "no" )
  if [ "${rhoas_logged_in}" == "no" ]; then
    echo "rhoas not logged in. Please log in with your Red Hat account."
    rhoas login --print-sso-url
  fi
  rhoas_logged_in=$( rhoas kafka list &> /dev/null && echo -n "yes" || echo -n "no" )
  if [ "${rhoas_logged_in}" == "no" ]; then
    die "rhoas login failure"
  else
    echo "rhoas logged in"
  fi
}

# create service accounts
function create_service_accounts {
  create_service_account "${ADMIN_SA_NAME}" "${ADMIN_SA_CREDENTIALS_FILE}"
  admin_sa_id=$( getManagedKafkaAdminSAClientId )
  if [ "${sa_updated}" == "yes" ] || [ "${kafka_created}" == "yes" ]; then
    rhoas kafka acl grant-admin -y --service-account "${admin_sa_id}"
    rhoas kafka acl create -y --user "${admin_sa_id}" --permission allow --operation create --topic all
    rhoas kafka acl create -y --user "${admin_sa_id}" --permission allow --operation delete --topic all
    echo "Admin account: ACLs created"
  fi

  create_service_account "${OPS_SA_NAME}" "${OPS_SA_CREDENTIALS_FILE}"
  ops_sa_id=$( getManagedKafkaOpsSAClientId )
  if [ "${sa_updated}" == "yes" ] || [ "${kafka_created}" == "yes" ]; then
    rhoas kafka acl create -y --user "${ops_sa_id}" --permission deny --operation alter --cluster
    rhoas kafka acl create -y --user "${ops_sa_id}" --permission deny --operation create --topic all
    rhoas kafka acl create -y --user "${ops_sa_id}" --permission deny --operation delete --topic all
    echo "Operational account: ACLs created"
  fi

  create_service_account "${MC_SA_NAME}" "${MC_SA_CREDENTIALS_FILE}"
  mc_sa_id=$( getManagedKafkaMcSAClientId )
  if [ "${sa_updated}" == "yes" ] || [ "${kafka_created}" == "yes" ]; then
    rhoas kafka acl grant-admin -y --service-account "${mc_sa_id}"
    rhoas kafka acl create -y --user "${mc_sa_id}" --permission allow --operation all --group all
    rhoas kafka acl create -y --user "${mc_sa_id}" --permission allow --operation all --topic all
    rhoas kafka acl create -y --user "${mc_sa_id}" --permission allow --operation all --transactional-id all
    echo "Managed Connector account: ACLs created"
  fi
}

function create_service_account {
  sa_name="$1"
  sa_credentials_file="$2"
  sa_count=$( rhoas service-account list -o json | jq -rc ".items[] | select( .name == \"${sa_name}\" )" | wc -l )
  sa_updated="no"

  if [ $sa_count -gt 1 ]; then
    die "there are ${sa_count} service accounts named \"${sa_name}\""
  elif [ $sa_count -eq 0 ]; then
    echo "Creating service account named \"${sa_name}\"..."
    rhoas service-account create --output-file="${sa_credentials_file}" --file-format=json --overwrite --short-description="${sa_name}"
    echo "Created service account named \"${sa_name}\""
    sa_updated="yes"
  else
    echo "Service account named \"${sa_name}\" found"
  fi

  sa_id=$( rhoas service-account list -o json | jq -rc ".items[] | select( .name == \"${sa_name}\" ) | .id" )
  if ! [ -f "${sa_credentials_file}" ]; then
    echo "No credentials file found for service account named \"${sa_name}\". Resetting credentials..."
    rhoas service-account reset-credentials --id "${sa_id}" --output-file="${sa_credentials_file}" --file-format=json -y
    sa_updated="yes"
  fi
}

# create kafka instance and wait for it to be ready
function create_kafka_instance_and_wait_ready {
  # create instance if not already existing
  instance_count=$( rhoas kafka list --search "${MANAGED_KAFKA_INSTANCE_NAME}" -o json | jq -rc ".items[] | select( .name == \"${MANAGED_KAFKA_INSTANCE_NAME}\" )" | wc -l )
  kafka_created="no"

  if [ $instance_count -gt 1 ]; then
    die "there are ${instance_count} instances named \"${MANAGED_KAFKA_INSTANCE_NAME}\""
  elif [ $instance_count -eq 0 ]; then
    local kafka_region=
    if [ ! -z "${MANAGED_KAFKA_REGION}" ]; then
      kafka_region="--region ${MANAGED_KAFKA_REGION}"
    fi
    echo "Creating Managed Kafka instance named \"${MANAGED_KAFKA_INSTANCE_NAME}\"..."
    rhoas kafka create -v ${kafka_region} --name "${MANAGED_KAFKA_INSTANCE_NAME}"
    echo "Created Managed Kafka instance named \"${MANAGED_KAFKA_INSTANCE_NAME}\""
    kafka_created="yes"
  else
    echo "Managed Kafka instance named \"${MANAGED_KAFKA_INSTANCE_NAME}\" found"
  fi

  # set instance as current
  instance_id=$( rhoas kafka describe --name "${MANAGED_KAFKA_INSTANCE_NAME}" -o json | jq -rc '.id' )
  rhoas kafka use --id "${instance_id}"

  # wait for instance to be ready
  kafka_status=$( rhoas kafka describe -o json | jq -rc '.status' )
  while [ "${kafka_status}" != "ready" ]; do
    echo "Waiting for Managed Kafka instance \"${MANAGED_KAFKA_INSTANCE_NAME}\" to become ready (current status \"${kafka_status}\")..."
    sleep 20
    kafka_status=$( rhoas kafka describe -o json | jq -rc '.status' )
  done
  echo "Managed Kafka instance \"${MANAGED_KAFKA_INSTANCE_NAME}\" is ${kafka_status}"

  # export information
  rhoas kafka describe --id "${instance_id}" -o json | jq -r > "${MANAGED_KAFKA_CREDENTIALS_FILE}"
}

rhoas_login
create_kafka_instance_and_wait_ready
create_service_accounts
