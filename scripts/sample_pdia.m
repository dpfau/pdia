function [pdias sampler] = sample_pdia(data,nSymb,samples,interval,burnIn,path)

if nargin < 7
    path = '/Users/davidpfau/Documents/Wood Group/PDIA/dist/PDIA.jar';
    if nargin < 6
        burnIn = 0;
        if nargin < 5
            interval = 1;
            if nargin < 4
                samples = 0;
                if nargin < 3
                    nSymb = 1;
                    for i = 1:length(data)
                        nSymb = max(max(data{i})+1,nSymb); % assume data goes from 0 up.
                    end
                end
            end
        end
    end
end

javaaddpath(path);

pdias = cell(samples,1);
dummy = java.lang.Object();
ja = java.lang.reflect.Array.newInstance(dummy.getClass(), numel(data));
for i = 1:numel(data)
    edu.columbia.stat.wood.pdia.Util.assignIntArray(ja, i-1, data{i});
end
sampler = edu.columbia.stat.wood.pdia.PDIA_Dirichlet.sample(nSymb,ja);