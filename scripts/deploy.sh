#!/usr/bin/env bash
set -euo pipefail

TARGET_BRANCH="${TARGET_BRANCH:-main}"
DEPLOY_REMOTE="${DEPLOY_REMOTE:-origin}"
SOURCE_REPO_DIR="${SOURCE_REPO_DIR:-$(pwd)}"
PAGES_ROOT="${PAGES_ROOT:-${SOURCE_REPO_DIR}/docs}"
UPDATE_SITE_DIR="${UPDATE_SITE_DIR:-${SOURCE_REPO_DIR}/releng/at.ac.tuwien.big.momot.update/target/repository}"
SITE_REPO_SUBPATH="${SITE_REPO_SUBPATH:-eclipse/updates/latest/develop}"
DEVELOP_INDEX_TEMPLATE="${PAGES_ROOT}/_templates/develop-index.md"
EXPECTED_FORK_SLUG="${EXPECTED_FORK_SLUG:-jku-win-se/MOMoT2}"
SKIP_PUSH="${SKIP_PUSH:-false}"

if [[ "${TRAVIS_PULL_REQUEST:-false}" != "false" ]]; then
    echo "Skipping deployment for pull request build."
    exit 0
fi

if [[ ! -d "${UPDATE_SITE_DIR}" ]]; then
    echo "Update site directory missing: ${UPDATE_SITE_DIR}" >&2
    exit 1
fi

if [[ ! -f "${UPDATE_SITE_DIR}/content.jar" && ! -f "${UPDATE_SITE_DIR}/content.xml" ]]; then
    echo "Missing p2 metadata: content.jar/content.xml not found in ${UPDATE_SITE_DIR}" >&2
    exit 1
fi

if [[ ! -f "${UPDATE_SITE_DIR}/artifacts.jar" && ! -f "${UPDATE_SITE_DIR}/artifacts.xml" ]]; then
    echo "Missing p2 metadata: artifacts.jar/artifacts.xml not found in ${UPDATE_SITE_DIR}" >&2
    exit 1
fi

if [[ ! -f "${DEVELOP_INDEX_TEMPLATE}" ]]; then
    echo "Missing develop index template: ${DEVELOP_INDEX_TEMPLATE}" >&2
    exit 1
fi

REMOTE_URL="$(git -C "${SOURCE_REPO_DIR}" remote get-url "${DEPLOY_REMOTE}")"
if [[ "${REMOTE_URL}" != *"github.com/${EXPECTED_FORK_SLUG}"* && "${REMOTE_URL}" != *"github.com:${EXPECTED_FORK_SLUG}"* ]]; then
    echo "Refusing publish: ${DEPLOY_REMOTE} (${REMOTE_URL}) is not the expected fork ${EXPECTED_FORK_SLUG}." >&2
    exit 1
fi

SHA="$(git -C "${SOURCE_REPO_DIR}" rev-parse --verify HEAD)"
DEPLOY_DIR="${PAGES_ROOT}/${SITE_REPO_SUBPATH}"

mkdir -p "${DEPLOY_DIR}"
rm -rf "${DEPLOY_DIR:?}"/*
cp -a "${UPDATE_SITE_DIR}"/. "${DEPLOY_DIR}/"
cp -f "${DEVELOP_INDEX_TEMPLATE}" "${DEPLOY_DIR}/index.md"

git -C "${SOURCE_REPO_DIR}" config user.name "${GIT_AUTHOR_NAME:-MOMoT Build Bot}"
git -C "${SOURCE_REPO_DIR}" config user.email "${GIT_AUTHOR_EMAIL:-momot-bot@users.noreply.github.com}"

if [[ -z "$(git -C "${SOURCE_REPO_DIR}" status --porcelain -- "docs/${SITE_REPO_SUBPATH}")" ]]; then
    echo "No update-site changes to publish; exiting."
    exit 0
fi

git -C "${SOURCE_REPO_DIR}" add "docs/${SITE_REPO_SUBPATH}"
git -C "${SOURCE_REPO_DIR}" commit -m "Deploy update site from ${SHA}"

if [[ "${SKIP_PUSH}" == "true" ]]; then
    echo "SKIP_PUSH=true; committed locally without pushing."
    exit 0
fi

git -C "${SOURCE_REPO_DIR}" push "${DEPLOY_REMOTE}" "HEAD:${TARGET_BRANCH}"
