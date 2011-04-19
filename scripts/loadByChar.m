function [dat alph] = loadByChar(path)

fid = fopen(path);
rawDat = fread(fid,inf);
fclose(fid);

newline = 10;
alph = [];
dat = {};
line = [];
idx = 1;
for i = 1:length(rawDat);
    if rawDat(i) == newline || i == length(rawDat)
%         dat{idx} = javaArray('java.lang.Integer',length(line));
%         for j = 1:length(line)
%             dat{idx}(j) = java.lang.Integer(line(j));
%         end
        dat{idx} = line;
        idx = idx + 1;
        line = [];
    else
        c = find(alph == rawDat(i),1);
        if isempty(c)
            line = [line length(alph)];
            alph = [alph rawDat(i)];
        else
            line = [line c-1];
        end
    end
end
            
