-- Removes hidden packages (e.g. root/Xposed managers) from
-- getInstalledPackages()/getInstalledApplications() results. The hidden
-- list is a comma-separated setting under this hook's own id; list
-- mutation itself happens in Kotlin (XParam:filterPackageList) since LuaJ
-- can't cleanly read public Java fields like PackageInfo.packageName.

function after(hookId, param)
    local hidden = param:getSetting(hookId)
    if hidden == nil then
        return false
    end
    param:filterPackageList(hidden)
    return true
end
