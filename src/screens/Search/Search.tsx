import React, {useState, useEffect} from 'react';

// componenet
import Card from '../../components/Card/Card';
import LoadCard from '../../components/LoadCard/LoadCard';
import SearchBar from '../../components/SearchBar/SearchBar';
import {useRequest} from 'apis/useRequest';
import {getAllJobsUsingGet, JobCardResponse} from 'apis/effimatch';
import useQueryParams from 'utils/useQueryParams';

// antd
import {Row, Col, Empty} from 'antd';
import {Tag} from 'antd';
import TweenOne from 'rc-tween-one';

// assets (temp)
import MS_logo from 'images/MS_logo.png';
import Avatar from 'images/avatar.png';

// style
import './styles/search.less';

const RenderCards = (
  cardsData?: JobCardResponse[],
  header?: boolean,
  load?: boolean,
) => {
  return (
    <div className="search-cards-wrapper">
      {header && (
        <div className="search-cards-title">
          <h1 className="search-cards-title-h1">Recent Jobs</h1>
        </div>
      )}
      <Row justify="space-between">
        {cardsData && !load
          ? cardsData?.map((item: JobCardResponse, i: number) => (
              <Col
                md={6}
                xs={24}
                className="search-cards-block"
                key={i.toString()}>
                <Card
                  title={item.job_title}
                  company={item.company_name}
                  logo={item.company_logo ?? MS_logo}
                  avatar={item.avatar ?? Avatar}
                  name={item.username ?? 'Referrer name'}
                  id={item.id}
                />
              </Col>
            ))
          : [1, 2, 3].map(ii => (
              <Col md={6} xs={24} className="home-card-block" key={ii}>
                <LoadCard key={ii} />
              </Col>
            ))}
      </Row>
      {cardsData?.length === 0 && !load ? <Empty /> : null}
    </div>
  );
};

const Search = () => {
  const [, setSearch] = useState(false);
  const [header, setHeader] = useState(true);
  const [load, setLoad] = useState(false);
  const [jobTag, setJob] = useState(new Array<string>()); // edited by William. The initial jobTab array should be empty

  const [getCardData, cardData] = useRequest(getAllJobsUsingGet);

  const query = useQueryParams();
  const [searchString, setSearchString] = useState(query.get('q') ?? '');

  useEffect(() => {
    const fetchData = async () => {
      await getCardData({
        pageNum: undefined,
        pageSize: 3,
        search: searchString,
      });
    };
    fetchData();
  }, [searchString]);

  const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

  const onClickSearch = async (value: string) => {
    // if not empty, add input as job tag
    if (value !== '') {
      setJob(jobTag => [...jobTag, value]);
    }

    setSearchString(value);

    setHeader(false);
    setLoad(true);
    setSearch(false);
    await wait(2000);
    setLoad(false);
    setSearch(true);
  };

  const handleClose = (removedItem: string, jobTag: string[]) => {
    const alternJobTag = jobTag.filter(word => word !== removedItem);
    setJob(alternJobTag);
  };

  return (
    <div className="search-wrapper">
      <div className="search-content-wrapper">
        <TweenOne animation={{x: 200, type: 'from', ease: 'linear'}}>
          <SearchBar buttonWidth={150} search={onClickSearch} />
        </TweenOne>

        <TweenOne animation={{x: -200, type: 'from', ease: 'linear'}}>
          {/* Tags. Temporarily dis-enabled */}
          {/* <div className="search-tags-wrapper">
            <Row justify="start">
              {jobTag.map((item: string, i: number) => (
                <Col className="search-tags-block" key={i.toString()}>
                  <Tag
                    closable
                    onClose={() => handleClose(item, jobTag)}
                    className="search-tags-tag">
                    {item}
                  </Tag>
                </Col>
              ))}
            </Row>
          </div> */}

          {RenderCards(cardData?.data, header, load)}
        </TweenOne>
      </div>
    </div>
  );
};

export default Search;
