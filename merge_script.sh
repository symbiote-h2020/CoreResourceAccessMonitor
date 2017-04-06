#!/bin/bash -e


: "${BRANCHES_TO_MERGE_REGEX?}" "${BRANCH_TO_MERGE?}"
: "${GITHUB_SECRET_TOKEN?}" "${GITHUB_REPO?}" "${REPO_TEMP?}"

export GIT_COMMITTER_EMAIL='travis@travis'
export GIT_COMMITTER_NAME='Travis CI'

if ! grep -q "$BRANCHES_TO_MERGE_REGEX" <<< "$TRAVIS_BRANCH"; then
    printf "Current branch %s doesn't match regex %s, exiting\\n" \
        "$TRAVIS_BRANCH" "$BRANCHES_TO_MERGE_REGEX" >&2
    exit 0
fi

# Since Travis does a partial checkout, we need to get the whole thing
git clone "https://github.com/$GITHUB_REPO" "$REPO_TEMP"

# shellcheck disable=SC2164
printf 'cd %s\n' "$REPO_TEMP" >&2
cd "$REPO_TEMP"

printf 'Checking out %s\n' "$BRANCHES_TO_MERGE_REGEX" >&2
git checkout "$BRANCHES_TO_MERGE_REGEX"

printf 'Merging %s\n' "$BRANCH_TO_MERGE" >&2
git pull origin "$BRANCH_TO_MERGE"

