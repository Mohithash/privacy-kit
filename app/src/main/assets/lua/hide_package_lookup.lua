-- getPackageInfo(name, flags) / getApplicationInfo(name, flags) are direct
-- single-item lookups, unlike getInstalledPackages/getInstalledApplications
-- which return a filterable list. Hiding a package here means making the
-- call behave as if it were never installed - thrown from "before" so the
-- real method never runs at all, rather than trying to alter its result.

function before(hookId, param)
    local hidden = param:getSetting(hookId)
    if hidden == nil then
        return false
    end

    local requested = param:getArgument(0)
    if requested == nil then
        return false
    end

    for name in string.gmatch(hidden, "[^,]+") do
        local trimmed = name:gsub("^%s+", ""):gsub("%s+$", "")
        if trimmed == requested then
            param:throwNameNotFound(requested)
            return true
        end
    end

    return false
end
