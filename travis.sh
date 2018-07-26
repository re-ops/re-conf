echo ">> lein travis"
lein travis
echo ">> lein cljsbuild test"
sudo lein cljsbuild test &> out
echo ">> grep for failures"
cat out
! grep FAIL out
