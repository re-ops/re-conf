lein travis
lein cljsbuild test &> out
cat out
! grep FAIL out
