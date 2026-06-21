-- Location.isFromMockProvider() always returns false once this hook fires,
-- regardless of any setting - hiding mock-location use isn't something a
-- per-app value would meaningfully customize, unlike a spoofed coordinate.

function after(hookId, param)
    local old = param:getResult()
    if old == false then
        return false
    end
    param:setResult(false)
    return true
end
