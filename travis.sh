lein travis
sudo "PATH=$PATH:/home/travis/.nvm/versions/node/v8.9.1/bin/" lein cljsbuild test &> out
cat out
! grep FAIL out
