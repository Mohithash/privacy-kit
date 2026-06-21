-- Shared script for both field hooks (e.g. Build.MODEL) and method hooks
-- (e.g. Build.getSerial()) - XParam abstracts the difference, so this script
-- doesn't need to know which kind of hook it's running for.
--
-- Setting key convention: the hook's own id (e.g. "device.build.model").
-- If no setting is configured for this app, the script does nothing and the
-- real value passes through untouched.

function after(hookId, param)
    local fake = param:getSetting(hookId)
    if fake == nil then
        return false
    end

    local old = param:getResult()
    param:setResult(fake)
    return true
end
