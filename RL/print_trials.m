function print_trials(f,n,arg,file)

a = fopen([file '.a'],'w');
o = fopen([file '.o'],'w');
r = fopen([file '.r'],'w');

for i = 1:n
    aor = f(arg{:});
    for j = 1:size(aor,1)
        fprintf(a,'%d',aor(j,1));
        fprintf(o,'%d',aor(j,2));
        fprintf(r,'%d',aor(j,3));
    end
    fprintf(a,'%c',10);
    fprintf(o,'%c',10);
    fprintf(r,'%c',10);
end

fclose(a);
fclose(o);
fclose(r);