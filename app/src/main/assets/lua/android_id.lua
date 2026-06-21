-- Settings.Secure.getString(resolver, name) is called for many different
-- setting names, not just ANDROID_ID - only override when the second
-- argument actually asks for android_id, otherwise every other secure
-- setting lookup in the app would silently break.

function after(hookId, param)
    local name = param:getArgument(1)
    if name ~= "android_id" then
        return false
    end

    local fake = param:getSetting(hookId)
    if fake == nil then
        return false
    end

    param:setResult(fake)
    return true
end
