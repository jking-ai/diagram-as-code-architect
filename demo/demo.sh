#!/usr/bin/env bash
#
# Diagram-as-Code Architect — Demo Script
#
# Usage: ./demo/demo.sh [BASE_URL]
#   BASE_URL defaults to https://diagram-architect-api-153583612125.us-central1.run.app
#
# Requires: curl, jq

set -euo pipefail

BASE_URL="${1:-https://diagram-architect-api-153583612125.us-central1.run.app}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Diagram-as-Code Architect — Demo${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "Target: ${BASE_URL}"
echo ""

# ---- Step 1: Health Check ----
echo -e "${BLUE}[1/5] Health Check${NC}"
HEALTH=$(curl -s "${BASE_URL}/api/v1/health")
STATUS=$(echo "${HEALTH}" | jq -r '.status')

if [ "${STATUS}" != "UP" ]; then
  echo -e "${RED}FAIL: Backend is not healthy. Status: ${STATUS}${NC}"
  echo "${HEALTH}" | jq .
  exit 1
fi

MODEL=$(echo "${HEALTH}" | jq -r '.components.chatModel.details.model')
echo -e "${GREEN}OK — Status: UP, Model: ${MODEL}${NC}"
echo ""

# Load sample files
JAVA_CODE=$(cat "${SCRIPT_DIR}/sample-order-service.java")
TF_CODE=$(cat "${SCRIPT_DIR}/sample-infrastructure.tf")

generate() {
  local TITLE="$1"
  local STEP="$2"
  local CODE="$3"
  local DIAGRAM_TYPE="$4"
  local CODE_LANGUAGE="$5"

  echo -e "${BLUE}[${STEP}] ${TITLE}${NC}"

  PAYLOAD=$(jq -n \
    --arg code "${CODE}" \
    --arg diagramType "${DIAGRAM_TYPE}" \
    --arg codeLanguage "${CODE_LANGUAGE}" \
    '{code: $code, diagramType: $diagramType, codeLanguage: $codeLanguage}')

  RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Content-Type: application/json" \
    -X POST "${BASE_URL}/api/v1/diagrams/generate" \
    -d "${PAYLOAD}")

  HTTP_CODE=$(echo "${RESPONSE}" | tail -1)
  BODY=$(echo "${RESPONSE}" | sed '$d')

  if [ "${HTTP_CODE}" != "200" ]; then
    echo -e "${RED}FAIL — HTTP ${HTTP_CODE}${NC}"
    echo "${BODY}" | jq . 2>/dev/null || echo "${BODY}"
    echo ""
    return 1
  fi

  PROCESSING_TIME=$(echo "${BODY}" | jq -r '.metadata.processingTimeMs')
  INPUT_CHARS=$(echo "${BODY}" | jq -r '.metadata.inputCharacters')
  echo -e "${GREEN}OK — ${INPUT_CHARS} chars in, ${PROCESSING_TIME}ms${NC}"
  echo ""
  echo "Mermaid output:"
  echo "${BODY}" | jq -r '.mermaidSyntax'
  echo ""
  echo "---"
  echo ""
}

# ---- Step 2: Java Flowchart ----
generate "Java Flowchart" "2/5" "${JAVA_CODE}" "FLOWCHART" "JAVA"

# ---- Step 3: Java Sequence Diagram ----
generate "Java Sequence Diagram" "3/5" "${JAVA_CODE}" "SEQUENCE" "JAVA"

# ---- Step 4: Java Class Diagram ----
generate "Java Class Diagram" "4/5" "${JAVA_CODE}" "CLASS" "JAVA"

# ---- Step 5: Terraform Infrastructure Diagram ----
generate "Terraform Infrastructure Diagram" "5/5" "${TF_CODE}" "INFRASTRUCTURE" "HCL"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Demo complete!${NC}"
echo -e "${GREEN}============================================${NC}"
