% actions      - 0 = left, 1 = right
% observations - 0 = middle, 1 = left end, 2 = right end
% rewards      - 0 = 0, 1 = -1, 2 = 10
function aors = linworld(n,len)

rl = 1; % move left or right (on average)
aors = zeros(len+1,4); % actions, observations, rewards and states
aors(1,4) = ceil(n/2);

as = 2*(rand(len,1) > 0.95)-1;
for i = 1:len
    aors(i,1) = as(i)*rl;
    aors(i+1,4) = aors(i,4) + aors(i,1);
    if aors(i+1,4) == 1 || aors(i+1,4) == n
        aors(i,3) = 2;
        if aors(i+1,4) == 1
            aors(i,2) = 1;
            rl = -1;
        else
            aors(i,2) = 2;
            rl = 1;
        end
    elseif aors(i+1,4) < 1
        aors(i+1,4) = 1;
        aors(i,2) = 1;
        rl = -1;
    elseif aors(i+1,4) > n
        aors(i+1,4) = n;
        aors(i,2) = 2;
        rl = 1;
    else
        aors(i,3) = 1;
    end
end

aors(:,1) = aors(:,1) > 0;
aors = aors(1:end-1,:);