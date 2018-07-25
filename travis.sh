lein travis
sudo lein cljsbuild test &> out
cat out
! grep FAIL out
